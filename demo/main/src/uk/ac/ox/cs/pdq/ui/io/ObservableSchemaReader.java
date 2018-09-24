package uk.ac.ox.cs.pdq.ui.io;

import static uk.ac.ox.cs.pdq.ui.PDQApplication.SCHEMA_FILENAME_SUFFIX;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.google.common.base.Preconditions;

//import uk.ac.ox.cs.pdq.SchemaReader;
import uk.ac.ox.cs.pdq.io.ReaderException;
import uk.ac.ox.cs.pdq.io.jaxb.IOManager;
import uk.ac.ox.cs.pdq.datasources.legacy.io.xml.AbstractXMLReader;
import uk.ac.ox.cs.pdq.datasources.legacy.io.xml.QNames;
import uk.ac.ox.cs.pdq.datasources.services.servicegroup.ServiceGroup;
import uk.ac.ox.cs.pdq.db.Schema;
import uk.ac.ox.cs.pdq.datasources.services.service.Service;
import uk.ac.ox.cs.pdq.ui.model.ObservableSchema;

// TODO: Auto-generated Javadoc
/**
 * Reads schemas from XML.
 * 
 * @author Julien LEBLAY
 * 
 */
public class ObservableSchemaReader {

	/** Logger. */
	private static Logger log = Logger.getLogger(ObservableSchemaReader.class);

	/** The name. */
	private String name;

	/** The description. */
	private String description;
	
	private Schema schema;

	private Service[] services;
	
	/** A conventional schema reader, service group. */
	private ServiceGroup sgr;
	
	/** A conventional schema reader, service. */
	private Service sr;

	/**
	 * Default constructor.
	 */
	public ObservableSchemaReader() {
	}

	/*
	 * (non-Javadoc)
	 * @see uk.ac.ox.cs.pdq.benchmark.io.AbstractReader#load(java.io.InputStream)
	 */
	public ObservableSchema read(File file) {
		try {
			File schemaDir = new File(file.getAbsolutePath() + "d");
			ArrayList<Service> list = new ArrayList<>();
			for(File serviceFile : listFiles(schemaDir, "", ".sr"))
			{
				File serviceGroupFile = new File(serviceFile.getAbsolutePath() + "g");
				JAXBContext jaxbContext1 = JAXBContext.newInstance(ServiceGroup.class);
				Unmarshaller jaxbUnmarshaller1 = jaxbContext1.createUnmarshaller();
				ServiceGroup sgr = (ServiceGroup) jaxbUnmarshaller1.unmarshal(serviceGroupFile);
				JAXBContext jaxbContext2 = JAXBContext.newInstance(Service.class);
				Unmarshaller jaxbUnmarshaller2 = jaxbContext2.createUnmarshaller();
				list.add((Service) jaxbUnmarshaller2.unmarshal(serviceFile));
			}
			this.schema = IOManager.importSchema(file);
			this.name = file.getPath();
			this.services = new Service[list.size()];
			for(int i = 0; i < list.size(); i++) services[i] = list.get(i);
			return new ObservableSchema(this.name, this.description, this.schema, this.services);
		} catch (JAXBException | FileNotFoundException e) {
			throw new ReaderException("Exception thrown while reading schema ", e);
		}
	}

	private static File[] listFiles(File directory, final String prefix, final String suffix) {
		Preconditions.checkArgument(directory.isDirectory(), "Invalid internal schema directory " + directory.getAbsolutePath());
		return directory.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.startsWith(prefix) && name.endsWith(suffix);
			}
		});
	}
	/**
	 * For test purpose only.
	 *
	 * @param args the arguments
	 */
	public static void main(String... args) {
		try (InputStream in = new FileInputStream("test/input/schema-mysql-tpch.xml")) {
/* MR			ObservableSchema s = new ObservableSchemaReader().read(in);
			new ObservableSchemaWriter().write(System.out, s); */
		} catch (IOException e) {
			log.error(e.getMessage(),e);
		}
	}
}
