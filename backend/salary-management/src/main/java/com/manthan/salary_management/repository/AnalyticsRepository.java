package com.manthan.salary_management.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class AnalyticsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Object[]> getSummary(String dimension, String metric) {
        String dimCol = mapDimension(dimension);
        String sql;
        
        if ("median".equalsIgnoreCase(metric)) {
            sql = "WITH RankedSalaries AS (" +
                  "    SELECT e." + dimCol + " as dim, " +
                  "           (SELECT sh.amount FROM salary_history sh WHERE sh.employee_id = e.id ORDER BY sh.effective_date DESC LIMIT 1) as current_salary," +
                  "           ROW_NUMBER() OVER(PARTITION BY e." + dimCol + " ORDER BY (SELECT sh.amount FROM salary_history sh WHERE sh.employee_id = e.id ORDER BY sh.effective_date DESC LIMIT 1)) as rn," +
                  "           COUNT(*) OVER(PARTITION BY e." + dimCol + ") as cnt " +
                  "    FROM employee e " +
                  "    WHERE e.status = 'ACTIVE'" +
                  ") " +
                  "SELECT dim, AVG(current_salary) " +
                  "FROM RankedSalaries " +
                  "WHERE rn IN (FLOOR((cnt + 1)/2), CEIL((cnt + 1)/2)) " +
                  "GROUP BY dim";
        } else if ("shareoftotal".equalsIgnoreCase(metric)) {
            sql = "SELECT e." + dimCol + ", " +
                  "SUM((SELECT sh.amount FROM salary_history sh WHERE sh.employee_id = e.id ORDER BY sh.effective_date DESC LIMIT 1)) / " +
                  "(SELECT SUM((SELECT sh2.amount FROM salary_history sh2 WHERE sh2.employee_id = e2.id ORDER BY sh2.effective_date DESC LIMIT 1)) FROM employee e2 WHERE e2.status = 'ACTIVE') * 100 " +
                  "FROM employee e " +
                  "WHERE e.status = 'ACTIVE' " +
                  "GROUP BY e." + dimCol;
        } else {
            String aggFunc = mapMetric(metric);
            sql = "SELECT e." + dimCol + ", " + aggFunc + "((SELECT sh.amount FROM salary_history sh WHERE sh.employee_id = e.id ORDER BY sh.effective_date DESC LIMIT 1)) " +
                  "FROM employee e " +
                  "WHERE e.status = 'ACTIVE' " +
                  "GROUP BY e." + dimCol;
        }

        Query query = entityManager.createNativeQuery(sql);
        return query.getResultList();
    }

    public List<Object[]> getTopEarners(int n, String order) {
        String dir = "desc".equalsIgnoreCase(order) ? "DESC" : "ASC";
        String sql = "SELECT e.id, e.employee_code, e.name, e.department, e.job_title, e.country, " +
                     "(SELECT sh.amount FROM salary_history sh WHERE sh.employee_id = e.id ORDER BY sh.effective_date DESC LIMIT 1) as current_salary " +
                     "FROM employee e " +
                     "WHERE e.status = 'ACTIVE' " +
                     "ORDER BY current_salary " + dir + " " +
                     "LIMIT :limit";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("limit", n);
        return query.getResultList();
    }

    public List<Object[]> getBrackets() {
        String sql = "SELECT " +
                     "  CASE " +
                     "    WHEN current_salary < 50000 THEN 'Under $50K' " +
                     "    WHEN current_salary BETWEEN 50000 AND 99999.99 THEN '$50K - $100K' " +
                     "    WHEN current_salary BETWEEN 100000 AND 149999.99 THEN '$100K - $150K' " +
                     "    ELSE '$150K+' " +
                     "  END as bracket, " +
                     "  COUNT(*) as count " +
                     "FROM (SELECT e.id, (SELECT sh.amount FROM salary_history sh WHERE sh.employee_id = e.id ORDER BY sh.effective_date DESC LIMIT 1) as current_salary FROM employee e WHERE e.status = 'ACTIVE') sub " +
                     "GROUP BY bracket " +
                     "ORDER BY MIN(current_salary)";
        Query query = entityManager.createNativeQuery(sql);
        return query.getResultList();
    }

    public Object[] getAvgVsMedian() {
        String sql = "WITH RankedSalaries AS (" +
                     "    SELECT (SELECT sh.amount FROM salary_history sh WHERE sh.employee_id = e.id ORDER BY sh.effective_date DESC LIMIT 1) as current_salary," +
                     "           ROW_NUMBER() OVER(ORDER BY (SELECT sh.amount FROM salary_history sh WHERE sh.employee_id = e.id ORDER BY sh.effective_date DESC LIMIT 1)) as rn," +
                     "           COUNT(*) OVER() as cnt " +
                     "    FROM employee e " +
                     "    WHERE e.status = 'ACTIVE'" +
                     ") " +
                     "SELECT " +
                     "  (SELECT AVG(current_salary) FROM RankedSalaries) as avg_val, " +
                     "  (SELECT AVG(current_salary) FROM RankedSalaries WHERE rn IN (FLOOR((cnt + 1)/2), CEIL((cnt + 1)/2))) as median_val";
        Query query = entityManager.createNativeQuery(sql);
        List<Object[]> result = query.getResultList();
        if (result.isEmpty()) return new Object[]{0, 0};
        return result.get(0);
    }

    private String mapDimension(String dimension) {
        switch (dimension.toLowerCase()) {
            case "department": return "department";
            case "country": return "country";
            case "jobtitle": return "job_title";
            case "status": return "status";
            default: throw new IllegalArgumentException("Invalid dimension");
        }
    }

    private String mapMetric(String metric) {
        switch (metric.toLowerCase()) {
            case "sum": return "SUM";
            case "avg": return "AVG";
            case "min": return "MIN";
            case "max": return "MAX";
            case "count": return "COUNT";
            default: throw new IllegalArgumentException("Invalid metric");
        }
    }
}
