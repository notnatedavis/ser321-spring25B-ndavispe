package example.grpcclient;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import service.*;
import service.ExerciseType;
import service.FitnessProto.*;
import service.ZodiacProto.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.google.protobuf.Empty; // needed to use Empty

public class Client {
  private final FitnessGrpc.FitnessBlockingStub blockingStub;
  private final ZodiacGrpc.ZodiacBlockingStub blockingStub2;
  private final RegistryGrpc.RegistryBlockingStub blockingStub3;
  private final RegistryGrpc.RegistryBlockingStub blockingStub4;
  private final LibraryServiceGrpc.LibraryServiceBlockingStub blockingStub5;

  /** Construct client for accessing server using the existing channel. */
  public Client(Channel channel, Channel regChannel) {
    // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's
    // responsibility to
    // shut it down.

    // Passing Channels to code makes code easier to test and makes it easier to
    // reuse Channels.
    blockingStub = FitnessGrpc.newBlockingStub(channel);
    blockingStub2 = ZodiacGrpc.newBlockingStub(channel);
    blockingStub3 = RegistryGrpc.newBlockingStub(regChannel);
    blockingStub4 = RegistryGrpc.newBlockingStub(channel);
    blockingStub5 = LibraryServiceGrpc.newBlockingStub(channel);
  }

  /** Construct client for accessing server using the existing channel. */
  public Client(Channel channel) {
    // 'channel' here is a Channel, not a ManagedChannel, so it is not this code's
    // responsibility to
    // shut it down.

    // Passing Channels to code makes code easier to test and makes it easier to
    // reuse Channels.
    blockingStub = FitnessGrpc.newBlockingStub(channel);
    blockingStub2 = ZodiacGrpc.newBlockingStub(channel);
    blockingStub5 = LibraryServiceGrpc.newBlockingStub(channel);
    blockingStub3 = null;
    blockingStub4 = null;
  }

  public void addExercise(String description, ExerciseType type) {
    Exercise exercise = Exercise.newBuilder()
            .setDescription(description)
            .setExerciseType(type)
            .build();
    AddRequest request = AddRequest.newBuilder().setExercise(exercise).build();
    AddResponse response = blockingStub.addExercise(request);
    System.out.println("Add Exercise Success: " + response.getIsSuccess());
  }

  public void getRandomExercise(ExerciseType type) {
    GetRequest request = GetRequest.newBuilder().setExerciseType(type).build();
    GetResponse response = blockingStub.getExercise(request);

    if (response.getIsSuccess()) {
      System.out.println("Exercise: " + response.getExercise().getDescription());
    } else {
      System.out.println("Error: " + response.getError());
    }
  }

  public void getZodiacSign(String name, String month, int day) {
    SignRequest request = SignRequest.newBuilder()
            .setName(name)
            .setMonth(month)
            .setDay(day)
            .build();
    SignResponse response = blockingStub2.sign(request);

    if (response.getIsSuccess()) {
      System.out.println("Zodiac: " + response.getMessage());
    } else {
      System.out.println("Error: " + response.getError());
    }
  }

  public void findZodiacEntries(String sign) {
    FindRequest request = FindRequest.newBuilder().setSign(sign).build();
    FindResponse response = blockingStub2.find(request);
    if (response.getIsSuccess()) {
      for (ZodiacEntry entry : response.getEntriesList()) {
        System.out.println("User: " + entry.getName() + ", Sign: " + entry.getSign());
      }
    } else {
      System.out.println("Error: " + response.getError());
    }
  }

  public void addBook(String title, List<String> genres, int pages) {
    Book request = Book.newBuilder()
            .setTitle(title)
            .addAllGenres(genres)
            .setPageCount(pages)
            .build();

    BookID response = blockingStub5.addBook(request);
    System.out.println("Added book ID: " + response.getId());
  }

  public void searchByGenre(String genre) {
    Books response = blockingStub5.findByGenre(
            GenreQuery.newBuilder().setGenre(genre).build()
    );

    System.out.println(genre + " Books (" + response.getBooksCount() + ")");
    System.out.println("Total pages : " + response.getTotalPages());
    for (Book book : response.getBooksList()) {
      System.out.println(book.getTitle());
      System.out.println("   ID: " + book.getIsbn());
      System.out.println("   Pages: " + book.getPageCount());
      System.out.println("   Genres: " + String.join(", ", book.getGenresList()));
    }
  }

  /* Dont touch below */
  public void getNodeServices() {
    GetServicesReq request = GetServicesReq.newBuilder().build();
    ServicesListRes response;
    try {
      response = blockingStub4.getServices(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void getServices() {
    GetServicesReq request = GetServicesReq.newBuilder().build();
    ServicesListRes response;
    try {
      response = blockingStub3.getServices(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  private static void handleFitness(Client client, BufferedReader reader) throws IOException {
    System.out.println("\n=== Fitness Service ===");
    System.out.println("Enter exercise description:");
    String desc = reader.readLine();
    System.out.println("Enter exercise type (CARDIO/STRENGTH/BALANCE):");
    ExerciseType type = ExerciseType.valueOf(reader.readLine().toUpperCase());
    client.addExercise(desc, type);
    client.getRandomExercise(type);
  }

  private static void handleZodiac(Client client, BufferedReader reader) throws IOException {
    System.out.println("\n=== Zodiac Service ===");
    System.out.println("Enter birth month (e.g., Jul):");
    String month = reader.readLine();
    System.out.println("Enter birth day:");
    int day = Integer.parseInt(reader.readLine());
    client.getZodiacSign("User", month, day);
    client.findZodiacEntries("Leo");
  }

  private static void handleLibrary(Client client, BufferedReader reader) throws IOException {
    System.out.println("\n=== Library Service ===");
    System.out.println("Enter book title:");
    String title = reader.readLine();
    System.out.println("Enter genres (comma-separated):");
    List<String> genres = Arrays.asList(reader.readLine().split(","));
    System.out.println("Enter page count:");
    int pages = Integer.parseInt(reader.readLine());
    client.addBook(title, genres, pages);

    System.out.println("\nSearch books by genre:");
    String genre = reader.readLine();
    client.searchByGenre(genre);
  }

  public void findServer(String name) {
    FindServerReq request = FindServerReq.newBuilder().setServiceName(name).build();
    SingleServerRes response;
    try {
      response = blockingStub3.findServer(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }

  public void findServers(String name) {
    FindServersReq request = FindServersReq.newBuilder().setServiceName(name).build();
    ServerListRes response;
    try {
      response = blockingStub3.findServers(request);
      System.out.println(response.toString());
    } catch (Exception e) {
      System.err.println("RPC failed: " + e);
      return;
    }
  }
  /* Dont touch above */

  public static void main(String[] args) throws Exception {
    // Setup
    if (args.length != 6) {
      System.out
          .println("Expected arguments: <host(String)> <port(int)> <regHost(string)> <regPort(int)> <message(String)> <regOn(bool)>");
      System.exit(1);
    }
    int port = 9099;
    int regPort = 9003;
    String host = args[0];
    String regHost = args[2];
    String message = args[4];
    try {
      port = Integer.parseInt(args[1]);
      regPort = Integer.parseInt(args[3]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port] must be an integer");
      System.exit(2);
    }

    // Create a communication channel to the server (Node), known as a Channel. Channels
    // are thread-safe
    // and reusable. It is common to create channels at the beginning of your
    // application and reuse
    // them until the application shuts down.
    String target = host + ":" + port;
    ManagedChannel channel = ManagedChannelBuilder.forTarget(target)
        // Channels are secure by default (via SSL/TLS). For the example we disable TLS
        // to avoid
        // needing certificates.
        .usePlaintext().build();

    String regTarget = regHost + ":" + regPort;
    ManagedChannel regChannel = ManagedChannelBuilder.forTarget(regTarget).usePlaintext().build();
    try {

      // ##############################################################################
      // ## Assume we know the port here from the service node it is basically set through Gradle
      // here.
      // In your version you should first contact the registry to check which services
      // are available and what the port
      // etc is.

      /**
       * Your client should start off with
       * 1. contacting the Registry to check for the available services
       * 2. List the services in the terminal and the client can
       *    choose one (preferably through numbering)
       * 3. Based on what the client chooses
       *    the terminal should ask for input, eg. a new sentence, a sorting array or
       *    whatever the request needs
       * 4. The request should be sent to one of the
       *    available services (client should call the registry again and ask for a
       *    Server providing the chosen service) should send the request to this service and
       *    return the response in a good way to the client
       *
       * You should make sure your client does not crash in case the service node
       * crashes or went offline.
       */

      // Just doing some hard coded calls to the service node without using the
      // registry
      // create client
      Client client = new Client(channel, regChannel);

      // ask the user for input how many jokes the user wants
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

      boolean exit = false;

      while (!exit) {
        System.out.println("\n=== Main Menu ===");
        System.out.println("1. Fitness Service");
        System.out.println("2. Zodiac Service");
        System.out.println("3. Library Service");
        System.out.println("4. Exit");
        System.out.print("Enter your choice: ");

        String input = reader.readLine().trim();
        int choice;
        try {
          choice = Integer.parseInt(input);
        } catch (NumberFormatException e) {
          System.out.println("Invalid input. Please enter a number.");
          continue;
        }

        switch (choice) {
          case 1:
            handleFitness(client, reader);
            break;
          case 2:
            handleZodiac(client, reader);
            break;
          case 3:
            handleLibrary(client, reader);
            break;
          case 4:
            exit = true;
            break;
          default:
            System.out.println("Invalid choice. Please select 1-4.");
        }
      }

      // list all the services that are implemented on the node that this client is connected to

      System.out.println("Services on the connected node. (without registry)");
      client.getNodeServices(); // get all registered services

      // ############### Contacting the registry just so you see how it can be done

      if (args[5].equals("true")) {
        // Comment these last Service calls while in Activity 1 Task 1, they are not needed and wil throw issues without the Registry running
        // get thread's services
        client.getServices(); // get all registered services

        client.findServer("services.Fitness/addExercise");

        client.findServers("services.Zodiac/sign");

        // does not exist
        client.findServer("random");
      }

    } finally {
      // ManagedChannels use resources like threads and TCP connections. To prevent
      // leaking these
      // resources the channel should be shut down when it will no longer be used. If
      // it may be used
      // again leave it running.
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      if (args[5].equals("true")) {
        regChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
      }
    }
  }
}
