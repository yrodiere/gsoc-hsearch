package io.github.mincongh.servlet;

import java.util.List;

import javax.ejb.EJB;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.logging.Logger;

import io.github.mincongh.entity.Address;
import io.github.mincongh.session.AddressSession;

@WebServlet("/search")
public class SearchServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    private Logger logger = Logger.getLogger(this.getClass());
    
    @EJB
    private AddressSession addressSession;
    
    protected void service(HttpServletRequest request,
            HttpServletResponse response) {
        
        List<Address> addresses = addressSession.getAddresses();
        if (addresses != null) {
            logger.info(addresses.size() + " rows found.");
            for (Address a : addresses) {
                logger.info(a);
            }
        }
    }
}
