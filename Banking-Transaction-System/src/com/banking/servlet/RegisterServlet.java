package com.banking.servlet;

import com.banking.dao.CustomerDAO;
import com.banking.dao.CustomerDAO.RegistrationResult;
import com.banking.model.Customer;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(RegisterServlet.class.getName());
    private final CustomerDAO customerDAO = new CustomerDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect("register.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String name = request.getParameter("name");
        String email = request.getParameter("email");
        String username = request.getParameter("username");
        String phone = request.getParameter("phone");
        String address = request.getParameter("address");
        String password = request.getParameter("password");
        String confirmPassword = request.getParameter("confirmPassword");

        // Never log passwords or password hashes.
        LOG.fine("Registration attempt received for username/email (values redacted in INFO logs).");

        if (name == null || name.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            username == null || username.trim().isEmpty() ||
            phone == null || phone.trim().isEmpty() ||
            address == null || address.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            request.setAttribute("errorMessage", "All fields are required!");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }

        if (!password.equals(confirmPassword)) {
            request.setAttribute("errorMessage", "Passwords do not match!");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }

        if (password.length() < 6) {
            request.setAttribute("errorMessage", "Password must be at least 6 characters long!");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            request.setAttribute("errorMessage", "Invalid email format!");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }

        String safeUser = safeId(username);
        try {
            if (customerDAO.isEmailExists(email.trim())) {
                request.setAttribute("errorMessage", "Email already registered!");
                request.getRequestDispatcher("register.jsp").forward(request, response);
                return;
            }

            if (customerDAO.isUsernameExists(username.trim())) {
                request.setAttribute("errorMessage", "Username already taken!");
                request.getRequestDispatcher("register.jsp").forward(request, response);
                return;
            }
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "Registration pre-check failed (user=" + safeUser + ", SQLState=" + e.getSQLState()
                    + ", code=" + e.getErrorCode() + ")", e);
            request.setAttribute("errorMessage", mapPrecheckError(e));
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Registration pre-check unexpected failure (user=" + safeUser + ")", e);
            request.setAttribute("errorMessage", "Registration service unavailable. Please try again later.");
            request.getRequestDispatcher("register.jsp").forward(request, response);
            return;
        }

        Customer customer = new Customer();
        customer.setName(name.trim());
        customer.setEmail(email.trim());
        customer.setUsername(username.trim());
        customer.setPhone(phone.trim());
        customer.setAddress(address.trim());
        customer.setPasswordHash(password);

        RegistrationResult result = customerDAO.registerCustomerDetailed(customer);

        if (result.isSuccess()) {
            LOG.info("Registration completed successfully (user=" + safeUser + ").");
            request.setAttribute("successMessage", "Registration successful! Please login.");
            request.getRequestDispatcher("login.jsp").forward(request, response);
        } else {
            LOG.warning("Registration failed (user=" + safeUser + ", code=" + result.getErrorCode() + ").");
            String msg = result.getUserMessage() != null ? result.getUserMessage() : "Registration failed! Please try again.";
            request.setAttribute("errorMessage", msg);
            request.getRequestDispatcher("register.jsp").forward(request, response);
        }
    }

    private static String mapPrecheckError(SQLException e) {
        String state = e.getSQLState();
        int code = e.getErrorCode();
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (code == 1045 || "28000".equals(state)) {
            return "Registration service unavailable (database authentication). Please contact support.";
        }
        if (code == 1054 || msg.contains("unknown column")) {
            return "Registration service unavailable (database schema). Please contact support.";
        }
        if (code == 0 && (msg.contains("communications link failure") || msg.contains("connection refused") || msg.contains("unknown database"))) {
            return "Registration service unavailable (database connection). Please try again later.";
        }
        return "Registration service unavailable. Please try again later.";
    }

    private static String safeId(String v) {
        if (v == null) return "null";
        String t = v.trim();
        if (t.length() > 64) t = t.substring(0, 64);
        return t.replaceAll("[^A-Za-z0-9@._\\-]", "_");
    }
}
