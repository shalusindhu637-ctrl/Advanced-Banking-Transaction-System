package com.banking.servlet;

import com.banking.dao.CustomerDAO;
import com.banking.dao.TransactionSecurityDAO;
import com.banking.dao.TransactionSecurityDAO.PinStatus;
import com.banking.model.Customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

@WebServlet("/balance")
public class BalanceServlet extends HttpServlet {

    private final CustomerDAO customerDAO = new CustomerDAO();
    private final TransactionSecurityDAO pinDao = new TransactionSecurityDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("customerId") == null) {
            response.sendRedirect(request.getContextPath() + "/login.jsp");
            return;
        }
        int customerId = (Integer) session.getAttribute("customerId"); // session-only
        Customer customer = customerDAO.getCustomerById(customerId);
        if (customer != null) session.setAttribute("customer", customer); // for nav/name; balance shown only after PIN via API
        PinStatus pin = pinDao.getPinStatus(customerId);
        request.setAttribute("hasPin", pin.hasPin);
        request.setAttribute("pinLocked", pin.locked);
        request.getRequestDispatcher("balance.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }
}
