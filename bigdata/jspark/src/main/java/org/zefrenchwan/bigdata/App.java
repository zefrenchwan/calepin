package org.zefrenchwan.bigdata;

import java.io.PrintStream;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) throws Exception {
        final String path = "D:\\popo.csv";
        try(PrintStream out = new PrintStream(path)) {
            out.println("DATE,PID,AMOUNT");

            for(int employee = 0; employee <= 5;  employee++) {
                for(int month = 1; month <= 12; month++) {
                    String date = "2024-";
                    if (month <= 9) {
                        date = date + "0";
                    }
                    date = date + Integer.toString(month) + "-01";

                    int money = 50000 + (employee % 2) * 10000 + (month * 1000);

                    String line = date + "," + Integer.toString(employee) + "," + Integer.toString(money);
                    out.println(line);
                }
            }
        }
    }
}
