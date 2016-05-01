package io.github.mincongh.servlet;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.logging.Logger;

import io.github.mincongh.session.AddressSession;

/**
 * Index entities for Hibernate Search
 * 
 * @author Mincong HUANG
 *
 */
@WebServlet("/index")
public class IndexServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    private Logger logger = Logger.getLogger(this.getClass());
    
    @EJB
    private AddressSession addressSession;

    protected void service(HttpServletRequest request,
            HttpServletResponse response) {
        try {
            addressSession.index();
        } catch (InterruptedException e) {
            logger.error(e);
        }
    }
}
