package com.banking.servlet;

import com.banking.dao.CustomerDAO;
import com.banking.model.Customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.logging.Logger;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(LoginServlet.class.getName());
    private final CustomerDAO customerDAO = new CustomerDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String usernameOrEmail = request.getParameter("username");
        String password = request.getParameter("password");

        // Never log passwords or password hashes.
        if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty() ||
            password == null || password.isEmpty()) {
            request.setAttribute("errorMessage", "Username/Email and password are required!");
            request.getRequestDispatcher("login.jsp").forward(request, response);
            return;
        }

        String id = usernameOrEmail.trim();
        com.banking.dao.CustomerDAO.AuthResult result = customerDAO.authenticate(id, password);
        if (result.isSuccess()) {
            LOG.info("Login succeeded (user=" + safeId(id) + ", customerId=" + result.getCustomer().getId() + ")");
            HttpSession session = request.getSession(true);
            session.invalidate();
            session = request.getSession(true);
            session.setAttribute("customer", result.getCustomer());
            session.setAttribute("customerId", result.getCustomer().getId());
            response.sendRedirect(request.getContextPath() + "/dashboard");
        } else {
            LOG.fine("Login failed (user=" + safeId(id) + ", code=" + result.getErrorCode() + ")");
            request.setAttribute("errorMessage", result.getMessage());
            request.getRequestDispatcher("login.jsp").forward(request, response);
        }
    }

    private static String safeId(String v) {
        if (v == null) return "null";
        String t = v.trim();
        if (t.length() > 64) t = t.substring(0, 64);
        return t.replaceAll("[^A-Za-z0-9@._\\-]", "_");
    }
}
