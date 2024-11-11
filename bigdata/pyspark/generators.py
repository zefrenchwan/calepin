from random import randint

def generate_salaries():
    with open("data/salaries.csv", "w") as f:
        f.write('DATE,RID,PID,AMOUNT\n')
        for year in range(2020, 2025):
            for month in range(1,12):
                # build the date 
                date = str(year) + "/"
                if month <= 9:
                    date = date + "0" + str(month)
                else:
                    date = date + str(month)
                date = date + "/01"
                # link role to person. 
                # Salary should be ascending 
                rid_pid = None 
                base_salary = 50000
                for index in range(0,10):
                    if index == 0:
                        rid_pid = '0,0'
                        base_salary = 50000
                    elif index >= 1 and index <= 4:
                        rid_pid = str(index) + "," + str(index)
                        base_salary = 50000 + ((5-index) * 5000)
                    else:
                        rid_pid = str(index) + ',0' 
                        base_salary = 50000
                    base_salary = base_salary + (year - 2020) * 3000
                    f.write(date + "," + rid_pid + "," + str(base_salary) + "\n")


def generate_sales():
    with open("data/sales.csv", "w") as f:
        f.write("ID,DATE,AMOUNT\n")
        
        index = 0
        for year in range(2020,2025):
            for month in range(1, 13):
                max_days = 31
                if month in [4,6,9,11]:
                    max_days = 30
                elif month == 2:
                    max_days = 28 if year % 4 != 0 else 29
                else:
                    max_days = 31

                for day in range(1, max_days + 1):
                    
                    date = str(year) + "/"
                    if month <= 9:
                        date = date + "0" + str(month)
                    else:
                        date = date + str(month)
                    date = date + "/"
                    if day <= 9:
                        date = date + "0" + str(day)
                    else:
                        date = date + str(day)

                    price = str(randint(10,500)) + "." + str(randint(1,99))
                    line = str(index) + "," + date + "," + price + "\n"
                    f.write(line)
                    index = index + 1


if __name__ == '__main__':
    generate_sales() 
                