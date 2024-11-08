if __name__ == '__main__':
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
                # Salary should be ascending values 
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



