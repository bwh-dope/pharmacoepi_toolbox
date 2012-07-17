package org.drugepi.util;
import java.io.FileOutputStream;
import java.util.Properties;

public class WriteTestingProperties {
	public static void main(String[] args)
	throws Exception {
		Properties properties = new Properties();
		properties.setProperty("DB_DRIVER", "org.netezza.Driver");
		properties.setProperty("DB_URL", "jdbc:netezza://dope-twinfin.partners.org/hdps");
		properties.setProperty("DB_USER", "jeremy");
		properties.setProperty("DB_PASSWORD", "nrova3030");
			
		properties.store(new FileOutputStream("testing.properties"), null);
	}
}
