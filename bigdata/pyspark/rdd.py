from pyspark import SparkContext, SparkConf
from sys import exit

conf = SparkConf().setAppName("test link rdd").setMaster("local[1]")
sc = SparkContext(conf=conf)

#########################
## STEP ONE: LOAD DATA ##
#########################

persons = sc.textFile("data/persons.csv") \
    .map(lambda line: line.strip()) \
    .filter(lambda line: len(line) >= 1) \
    .filter(lambda line: line != "ID,NAME") \
    .map(lambda line: line.split(",")) 

# no show, just take (returns a list) and print  
#for value in persons.take(10):
#    print(value)

roles = sc.textFile("data/roles.csv") \
    .map(lambda line: line.strip()) \
    .filter(lambda line: len(line) >= 1) \
    .filter(lambda line: line != "ID,NAME") \
    .map(lambda line: line.split(",")) 
    
salaries = sc.textFile("data/salaries.csv") \
    .map(lambda line: line.strip()) \
    .filter(lambda line: len(line) >= 1) \
    .filter(lambda line: line != "DATE,RID,PID,AMOUNT") \
    .map(lambda line: line.split(",")) 

#########################
## STEP TWO: make join ##
#########################

# salaries -> pid, (rid, date, amount) because RDD joins work with pair, rest is just lost. 
# For instance, when mapped to pid, rid, date, amount, salaries join persons would return key, rid, name (rest is lost). 
# Then, given (k,u).join((k,v)), result is (k,(u,v)) and not k,u,v
# For instance, before the last map, situation is: ('0', (('0', '2020/01/01', '50000'), 'John Doe'))
# We want to return  rid, (pid, name, date, amount), hence the last map

def makeRoleBased(v): 
    pid = v[0]
    person = v[1][1]
    salaries = v[1][0]
    rid = salaries[0]
    date = salaries[1]
    amount = salaries[2]
    return (rid, (pid, person, date, amount))    

# apply it to the join 
person_salaries = salaries.map(lambda v: (v[2], (v[1], v[0], v[3]))).join(persons).map(makeRoleBased) 

#for value in person_salaries.take(10):
#    print(value)

# then, join with roles by role id 
# Simple joins would return ('0', (('0', 'John Doe', '2020/01/01', '50000'), 'Dev'))
# same principle, use a function to deal with map result
def mapRoleValues(v):
    values = v[1]
    salaries = values[0]
    role = values[1]
    pid = salaries[0]
    name = salaries[1]
    date = salaries[2]
    amount = salaries[3]
    return (role, pid, name, date, amount)

role_person_salary = person_salaries.join(roles).map(mapRoleValues)

#for v in role_person_salary.take(10):
#    print(v)
#Example: 
#('Dev', '0', 'John Doe', '2020/01/01', '50000')
#('Dev', '0', 'John Doe', '2020/02/01', '50000')
#('Dev', '0', 'John Doe', '2020/03/01', '50000')
#('Dev', '0', 'John Doe', '2020/04/01', '50000')

# and then, use that data for stats

# clever one: use reduce by key to produce average (no need to use group by key)
avg_by_key = role_person_salary.map(lambda v: (v[0],v[4])) \
    .mapValues(lambda v: (float(v), 1)) \
    .reduceByKey(lambda a,b: (a[0]+b[0], a[1]+b[1])) \
    .mapValues(lambda v: v[0]/v[1]) 


#for k,v in avg_by_key.collectAsMap().items():
#    print(k,v)
# produces
# HR 61000.0
# Dev 56000.0
# Accountant 66000.0
# CTO 71000.0
# CEO 76000.0