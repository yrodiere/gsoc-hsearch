import io.github.mincongh.entity.Address;
import io.github.mincongh.service.AddressService;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class App {

	public static void main(String[] args) {

		// Create entity manager factory from Persistence
		EntityManagerFactory emf = 
				Persistence.createEntityManagerFactory("jse-chunk");

		// Create address service for Address entity's operations
		AddressService addressService = new AddressService(emf);
		addressService.open();
		for (Address a : addressService.getAddresses()) {
			System.out.println(a);
		}

		// Close everything
		addressService.close();
		emf.close();
	}
}
