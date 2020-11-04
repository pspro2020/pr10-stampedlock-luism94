package codes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;

public class Almacen implements Runnable{
	
	private List<Producto> stock = new ArrayList<Producto>();
	private StampedLock cerrojo = new StampedLock();
	private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

	@Override
	public void run() {
		//Mientras el hilo del almacen no sea interrumpido ira colocando un producto elegido
		//aleatoriamente cada 2 segundos
		while (!Thread.currentThread().isInterrupted()) {
			try {
				Random random = new Random();
				int producto = random.nextInt(3) + 1;
				colocarProducto(producto);
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e) {
				//El hilo muestra un mensaje y termina su ejecucion cuando es interrumpido
				System.out.println("El almacen se ha interrumpido");
				return;
			} 
		}
	}


	private void colocarProducto(int producto) {
		//Se crea un cerrojo de escritura y se bloquea la entrada de los demas hilos en las propiedades del almacen
		long stamp = cerrojo.writeLock();
		
		try {
			//Se crea un producto con el ID recibido por parametro y se añade a la lista
			stock.add(new Producto(producto));
			System.out.println(LocalDateTime.now().format(formatter) + " --- El almacen ha añadido un producto nº " + producto);
		} finally {
			//Se desbloquea el cerrojo y el hilo termina su ejecucion
			cerrojo.unlockWrite(stamp);
		}
		
	}


	public void consultarProducto(int productoID) {
		int cantidadProducto = 0;
		//El hilo que vaya a hacer una lectura intenta obtener el cerrojo de forma optimista
		long stamp = cerrojo.tryOptimisticRead();
		
		if (!cerrojo.validate(stamp)) {
			//Si otro hilo ha obtenido el cerrojo de forma exclusiva, la anterior estampa ya no es valida
			//El hilo obtendra el cerrojo de forma pesimista para leer cuando este disponible
			stamp = cerrojo.readLock();
			
			try {
				
				for (Producto producto : stock) {
					if (producto.getProductoId() == productoID) {
						cantidadProducto++;	
					}
				}
				
			} finally {
				//Cuando se ha terminado de leer de forma exclusiva se libera el cerrojo
				cerrojo.unlockRead(stamp);
			}
		} else {
			//Si la anterior estampa es valida se lee de forma normal la lista
			//No se libera el cerrojo porque la lectura optimista no es bloqueante
			for (Producto producto : stock) {
				if (producto.getProductoId() == productoID) {
					cantidadProducto++;	
				}
			}
		}
		//Se muestra el mensaje por pantalla
		System.out.println(LocalDateTime.now().format(formatter) + " --- El empleado " + productoID + " ha contado " + cantidadProducto + " del producto " + productoID);
	}
}
