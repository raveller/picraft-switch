package com.huck13.picraftswitch;

import org.apache.commons.io.FileUtils;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.CopyOption;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

@RestController
public class SwitchController {
 
	public enum WorldType implements Serializable {
		VANILLA,
		PAPER;
	}

	private static String vanillaWorldsPath;
	private static String vanillaStartCommand;
	private static String vanillaBaseTemplate;
	private static String paperWorldsPath;
	private static String paperStartCommand;
	private static String paperBaseTemplate;
	private static String stopCommand;
	private static boolean startRequested;
	
	static {
		vanillaWorldsPath = System.getenv("PICRAFT_VANILLA_WORLDS_PATH");
		vanillaStartCommand = System.getenv("PICRAFT_VANILLA_START_COMMAND");
		vanillaBaseTemplate = System.getenv("PICRAFT_VANILLA_TEMPLATE");
		paperWorldsPath = System.getenv("PICRAFT_PAPER_WORLDS_PATH");
		paperStartCommand = System.getenv("PICRAFT_PAPER_START_COMMAND");	
		paperBaseTemplate = System.getenv("PICRAFT_PAPER_TEMPLATE");	
		stopCommand = System.getenv("PICRAFT_STOP_COMMAND");
		System.out.printf("\tVanilla Worlds Path:    %s\n", vanillaWorldsPath);
		System.out.printf("\tVanilla Start Command:  %s\n", vanillaStartCommand);
		System.out.printf("\tVanilla Base Prop file: %s\n", vanillaBaseTemplate);
		System.out.printf("\tPaper Worlds Path:      %s\n", paperWorldsPath);
		System.out.printf("\tPaper Start Command:    %s\n", paperStartCommand);
		System.out.printf("\tPaper Base Prop file:   %s\n", paperBaseTemplate);
		System.out.printf("\tStop Command:           %s\n", stopCommand);
	}
	
	
	public static class WorldDetail implements Serializable, Comparable<WorldDetail> {

		private static final long serialVersionUID = 4235557375348814885L;

		public String worldId;
		public WorldType type;
		public String gamemode;
		public String messageOfTheDay;
		public String levelName;
		public String levelSeed;
		
		@Override
		public int compareTo(WorldDetail o) {
			return worldId.compareTo(((WorldDetail)o).worldId);
		}
	}
	
    
    @GetMapping("/worlds")
	public List<WorldDetail> worldsList () {        

		ArrayList<WorldDetail> worlds = new ArrayList<WorldDetail>();
		
		System.out.printf("Listing files from %s\n",  vanillaWorldsPath);
		File baseDir = new File(vanillaWorldsPath);		
		for (File file : baseDir.listFiles()) {
			if (file.getPath().equals(vanillaBaseTemplate))
				continue;
			
			if (file.isDirectory())
			{	
				WorldDetail detail = new WorldDetail();
				detail.type = WorldType.VANILLA;

				detail.worldId = Paths.get(file.toURI()).getFileName().toString();
				var propFile = new File(file.getAbsolutePath() + "/server.properties");
				if (propFile.exists() && propFile.isFile()) {

					System.out.printf("\tOpening server properties from %s\n",  propFile.getAbsolutePath());
			        try (InputStream input = new FileInputStream(propFile.getAbsolutePath())) {

			            Properties prop = new Properties();

			            // load a properties file
			            prop.load(input);

			            detail.gamemode = prop.getProperty("gamemode");
			            detail.levelName = prop.getProperty("level-name");
			            detail.levelSeed = prop.getProperty("level-seed");
			            detail.messageOfTheDay = prop.getProperty("motd");

			        } catch (IOException ex) {
			            ex.printStackTrace();
			        }
				}
						
				worlds.add(detail);
			}
		}
		System.out.printf("Listing files from %s\n",  paperWorldsPath);
		baseDir = new File(paperWorldsPath);
		for (File file : baseDir.listFiles()) {
			if (file.getPath().equals(paperBaseTemplate))			
				continue;			

			if (file.isDirectory())
			{
				WorldDetail detail = new WorldDetail();
				detail.type = WorldType.PAPER;
				detail.worldId = Paths.get(file.toURI()).getFileName().toString();
				var propFile = new File(file.getAbsolutePath() + "/server.properties");
				if (propFile.exists() && propFile.isFile()) {

					System.out.printf("\tOpening server properties from %s\n",  propFile.getAbsolutePath());
			        try (InputStream input = new FileInputStream(propFile.getAbsolutePath())) {

			            Properties prop = new Properties();

			            // load a properties file
			            prop.load(input);

			            detail.gamemode = prop.getProperty("gamemode");
			            detail.levelName = prop.getProperty("level-name");
			            detail.levelSeed = prop.getProperty("level-seed");
			            detail.messageOfTheDay = prop.getProperty("motd");

			        } catch (IOException ex) {
			            ex.printStackTrace();
			        }
				}
						
				worlds.add(detail);
			}
		}
		Collections.sort(worlds);
		
        return worlds;
    }

    @GetMapping("/status")
	public List<String> status () {        

    	MineStat ms = new MineStat("localhost", 25565);

    	List<String> status = new ArrayList<String>();
    	if (!ms.isServerUp()) {
    		status.add("Server is not up!");
    		return status;
    	}
    	
    	if (startRequested)
    		startRequested = false;
    	
    	status.add("Server is online running version " + ms.getVersion() + " with " + ms.getCurrentPlayers() + " out of " + ms.getMaximumPlayers() + " players.");
    	status.add("Message of the day: " + ms.getMotd());
    	status.add("Latency: " + ms.getLatency() + "ms");
          
        return status;
    }

	@PostMapping("/start/{worldId}/{worldType}")
    public String startServer (@PathVariable String worldId, @PathVariable WorldType worldType) {        
		try {
	    	MineStat ms = new MineStat("localhost", 25565);
	    	
	    	if (ms.isServerUp())
	    		return "Server is already running";

	    	if (startRequested)
	    		return "Start request in progress";
	    	
			String command;
			if (worldType == WorldType.PAPER)
				command = String.format("%s/%s/%s",paperWorldsPath, worldId, paperStartCommand);
			else
				command = String.format("%s/%s %s", vanillaWorldsPath, vanillaStartCommand, worldId);				

			System.out.printf("%s Start command\n\t%s\n",worldType.toString(), command);
			Process start = Runtime.getRuntime().exec(command);				
			int exitCode = start.waitFor();
			
			if (exitCode == 0) {
				startRequested = true;
				return "Server start requested";
			}

			return "Error starting server (3)";

		} catch (IOException e) {
			e.printStackTrace();
			return "Error starting server (4)";
		} catch (InterruptedException e) {
			System.out.println("Interrupted Exception YO");
			e.printStackTrace();
			return "Error starting server (4)";
		}
		
    }
	 
		@PostMapping("/stop")
	    public String stopServer () {
			try {
		    	MineStat ms = new MineStat("localhost", 25565);
		    	
		    	if (!ms.isServerUp())
		    		return "Server is not running";

		    	if (startRequested)
		    		return "Start request in progress";
		    	
				Process stop = Runtime.getRuntime().exec(stopCommand);
				
				int exitCode = stop.waitFor();

				if (exitCode == 0)
					return "Server stop requested";

				return "Error stopping server (0)";
			} catch (IOException e) {
				e.printStackTrace();
				return "Error stopping server (1)";
			} catch (InterruptedException e) {
				e.printStackTrace();
				return "Error stopping server (2)";
			}
	    }

	@PostMapping("/create")
    public ResponseEntity<String> createWorld (@RequestBody WorldDetail detail) {  
		detail.type = WorldType.PAPER;
		var basePath = paperWorldsPath;
		var baseDir= new File(basePath);
		if (!baseDir.exists()) {
			var responseStr = String.format("Base directory for '%s' does not exist.", detail.type);
			System.out.print("ERROR - ");
			System.out.println(responseStr);
			return new ResponseEntity<String>(responseStr, HttpStatus.INTERNAL_SERVER_ERROR );
		}

		if (!baseDir.isDirectory()) {
			var responseStr = String.format("'%s' base directory is not a directory, but instead a file.", detail.type);
			System.out.print("ERROR - ");
			System.out.println(responseStr);
			return new ResponseEntity<String>(responseStr, HttpStatus.INTERNAL_SERVER_ERROR );
		}
		var baseTemplatePath = paperBaseTemplate;
		var baseTemplateDir = new File(baseTemplatePath);
		if (!baseTemplateDir.exists()) {
			var responseStr = String.format("Base template directory for '%s' does not exist.", detail.type);
			System.out.print("ERROR - ");
			System.out.println(responseStr);
			return new ResponseEntity<String>(responseStr, HttpStatus.INTERNAL_SERVER_ERROR );
		}

		if (!baseTemplateDir.isDirectory()) {
			var responseStr = String.format("'%s' 'server.properties' file is not a directory.", detail.type);
			System.out.print("ERROR - ");
			System.out.println(responseStr);
			return new ResponseEntity<String>(responseStr, HttpStatus.INTERNAL_SERVER_ERROR );
		}
		
		var createPath = String.format("%s/%s", basePath, detail.worldId);
		var createDir = new File(createPath);
		if (createDir.exists()) {
			var responseStr = String.format("Path for '%s' for type '%s' already exists.", detail.worldId, detail.type);
			System.out.print("ERROR - ");
			System.out.println(responseStr);
			return new ResponseEntity<String>(responseStr, HttpStatus.INTERNAL_SERVER_ERROR );
		}

		try {
            Files.walkFileTree(Path.of(baseTemplatePath), EnumSet.of(FileVisitOption.FOLLOW_LINKS),
                Integer.MAX_VALUE, new CopyDirectory(Path.of(baseTemplatePath), Path.of(createPath)));
        }
        catch (IOException ex) {
			var responseStr = String.format("Error creating new directory for '%s' from template directory.", detail.worldId);
			System.out.print("ERROR - ");
			System.out.println(responseStr);
            ex.printStackTrace();
			return new ResponseEntity<String>(responseStr, HttpStatus.INTERNAL_SERVER_ERROR );
        }
        
		return modifyServerProperties(detail);
    }

	@PutMapping("/modify")
    public ResponseEntity<String> modifyWorld (@RequestBody WorldDetail detail) {  
		return modifyServerProperties(detail);
		   
    }
 
 
	public ResponseEntity<String> modifyServerProperties(WorldDetail detail) {
		var basePath = detail.type == WorldType.VANILLA ? vanillaWorldsPath : paperWorldsPath;
		var filePath = String.format("%s/%s/server.properties", basePath, detail.worldId);
		var propFile = new File(filePath);
		if (!propFile.exists()) {
			var responseStr = String.format("File '%s' does not exist.", detail.worldId);
			System.out.print("ERROR - ");
			System.out.println(responseStr);
			return new ResponseEntity<String>(responseStr, HttpStatus.INTERNAL_SERVER_ERROR );
		}

		if (!propFile.isFile()) {
			var responseStr = String.format("'%s' is not a file.", detail.worldId);
			System.out.print("ERROR - ");
			System.out.println(responseStr);
			return new ResponseEntity<String>(responseStr, HttpStatus.INTERNAL_SERVER_ERROR );
		}

		System.out.printf("\tOpening server properties from %s\n",  propFile.getAbsolutePath());
        var prop = new Properties();

        try (var input = new FileInputStream(propFile.getAbsolutePath())) {

            // load a properties file
            prop.load(input);
		} catch (IOException ex) {
			var responseStr = String.format("Error reading properties from '%s' properties file.", detail.worldId);
			System.out.print("ERROR - ");
			System.out.println(responseStr);
            ex.printStackTrace();
			return new ResponseEntity<String>(responseStr, HttpStatus.INTERNAL_SERVER_ERROR );
        }
        

		System.out.printf("Modifying properties\n");
        prop.setProperty("gamemode", detail.gamemode); 
        prop.setProperty("level-name", detail.levelName); 
        prop.setProperty("level-seed", detail.levelSeed); 
        prop.setProperty("motd", detail.messageOfTheDay); 

		System.out.printf("\tStoring updated server properties to %s\n",  propFile.getAbsolutePath());

        try (var output = new FileOutputStream(propFile.getAbsolutePath())) {

            // load a properties file
            prop.store(output, "");

    		return new ResponseEntity<String>("Success", HttpStatus.OK );
            
        } catch (IOException ex) {

			var responseStr = String.format("Error writing updated properties to '%s' properties file.", detail.worldId);
			System.out.print("ERROR - ");
			System.out.println(responseStr);
            ex.printStackTrace();
			return new ResponseEntity<String>(responseStr, HttpStatus.INTERNAL_SERVER_ERROR );
        }        
	}
	
	public class CopyDirectory extends SimpleFileVisitor<Path> {

	    private Path source;
	    private Path target;

	    public CopyDirectory(Path source, Path target) {
	      this.source = source;
	      this.target = target;
	    }

	    @Override
	    public FileVisitResult preVisitDirectory(final Path dir,
	    final BasicFileAttributes attrs) throws IOException {
	    	Path targetDirectory = target.resolve(source.relativize(dir));
	        try {
	          System.out.println("Copying " + source.relativize(dir));
	          Files.copy(dir, targetDirectory);
	        } catch (FileAlreadyExistsException e) {
	          if (!Files.isDirectory(targetDirectory)) {
	            throw e;
	          }
	        }
	        return FileVisitResult.CONTINUE;
	    }

	    @Override
	    public FileVisitResult visitFile(final Path file,
	    final BasicFileAttributes attrs) throws IOException {
	    Files.copy(file,
	        target.resolve(source.relativize(file)), StandardCopyOption.COPY_ATTRIBUTES);
	    return FileVisitResult.CONTINUE;
	    }
	}

}