package org.zefrenchwan.bigdata;

import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.spark.api.java.function.FilterFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JoinTest implements Serializable {

    @Test 
    public void testJoins() throws Exception {
        final URI employeesURI = JoinTest.class.getClassLoader().getResource("employees.csv").toURI();
        final URI personsURI = JoinTest.class.getClassLoader().getResource("persons.csv").toURI();
        SparkSession session = Sessions.getSession();
        Dataset<Row> persons = session.read().option("header", true).csv(personsURI.getPath());
        Dataset<Row> employees = session.read().option("header", true).csv(employeesURI.getPath());
        Dataset<Row> roles_per_person = persons
            .join(employees, persons.col("id").equalTo(employees.col("pid")))
            .select("PID", "FNAME","LNAME","ROLE","FROM","TO")
            .withColumnRenamed("PID", "ID")
            .orderBy("ID","FROM");
        // find most recent roles  
        Dataset<Row> current_roles = roles_per_person.filter(new FilterFunction<Row>() {
            @Override
            public boolean call(Row value) throws Exception {
                final String date = value.getString(5); 
                return date == null || date.length() != "yyyy-mm-dd".length();
            }            
        }).select("FNAME","LNAME","ROLE");

        current_roles.show();
        
        List<String> values = current_roles
            .collectAsList().stream()
            .map(row -> row.getString(0) + ":" + row.getString(1) + "->" + row.getString(2))
            .sorted()
            .collect(Collectors.toList());

        List<String> expected = Arrays.asList(
            "Adrian:Smith->Dev", "François:Dupont->Dev", "Jane:Doe->CEO", "Nicolas:Fabre->Head of Something", "Robert:Durand->Dev"
        );

        Assertions.assertEquals(expected, values);
    }
    
}
