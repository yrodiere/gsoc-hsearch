package io.github.mincongh.servlet;

import java.io.IOException;
import java.util.List;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.logging.Logger;

import io.github.mincongh.entity.Address;
import io.github.mincongh.session.SearchSession;

@WebServlet("/search")
public class SearchServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    private Logger logger = Logger.getLogger(this.getClass());
    
    @EJB
    private SearchSession searchSession;
    
    protected void service(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        
        String searchString = request.getParameter("q");
        String queryType = request.getParameter("type");
        List<Address> addresses = searchSession.search(queryType, searchString);
        if (addresses != null) {
            logger.info(addresses.size() + " rows found.");
            for (Address a : addresses) {
                logger.info(a);
            }
            // place search result to request
            request.setAttribute("addresses", addresses);
        }
        // Pass the request object to the JSP / JSTL view for rendering
        getServletContext()
                .getRequestDispatcher("/WEB-INF/page/search-results.jsp")
                .forward(request, response);
    }
}
