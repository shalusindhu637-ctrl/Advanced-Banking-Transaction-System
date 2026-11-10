package com.banking.servlet;

import com.banking.dao.CustomerDAO;
import com.banking.model.Customer;
import com.banking.model.Transaction;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {
    
    private CustomerDAO customerDAO = new CustomerDAO();
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("customerId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }
        
        int customerId = (Integer) session.getAttribute("customerId");
        Customer customer = customerDAO.getCustomerById(customerId);
        session.setAttribute("customer", customer);
        
        List<Transaction> transactions = customerDAO.getTransactionHistory(customerId);
        request.setAttribute("recentTransactions", transactions.size() > 5 ? transactions.subList(0, 5) : transactions);
        
        // Calculate totals
        double totalDeposit = 0;
        double totalWithdrawal = 0;
        for (Transaction t : transactions) {
            if ("DEPOSIT".equals(t.getTransactionType())) {
                totalDeposit += t.getAmount();
            } else if ("WITHDRAWAL".equals(t.getTransactionType())) {
                totalWithdrawal += t.getAmount();
            }
        }
        request.setAttribute("totalDeposit", totalDeposit);
        request.setAttribute("totalWithdrawal", totalWithdrawal);
        
        request.getRequestDispatcher("dashboard.jsp").forward(request, response);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        doGet(request, response);
    }
}
