package example.grpcclient;

import service.LibraryServiceGrpc;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import io.grpc.ServerBuilder;
import io.grpc.ServerMethodDefinition;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import service.*;
import java.util.Stack;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.ArrayList;
import java.util.stream.Collectors;

import buffers.RequestProtos.Request;
import buffers.RequestProtos.Request.RequestType;
import buffers.ResponseProtos.Response;

public class LibraryImpl extends LibraryServiceGrpc.LibraryServiceImplBase {

    // implement Fitness service from .proto
    // rpc addBook
    // rpc findByGenre

    private final Map<String, Book> books = new ConcurrentHashMap<>();
    private static final String PERSISTENCE_FILE = "books.txt";

    public LibraryImpl() {
        loadFromFile(); // load existing data on startup
    }

    @Override
    public void addBook(Book request, StreamObserver<BookID> responseObserver) {
        // generate unique ID
        String isbn = UUID.randomUUID().toString();

        // create book with ID (lowercase genres)
        Book bookWithId = request.toBuilder()
                .setIsbn(isbn)
                .clearGenres()
                .addAllGenres(
                        request.getGenresList().stream()
                                .map(String::toLowerCase)
                                .collect(Collectors.toList())
                )
                .build();

        // persist to file
        persistBook(bookWithId);

        // respond
        responseObserver.onNext(BookID.newBuilder().setId(isbn).build());
        responseObserver.onCompleted();
    }

    @Override
    public void findByGenre(GenreQuery request, StreamObserver<Books> responseObserver) {
        List<Book> matches = new ArrayList<>();
        int totalPages = 0;

        for (Book book : books.values()) {
            if (book.getGenresList().contains(request.getGenre().toLowerCase())) {
                matches.add(book);
                totalPages += book.getPageCount();
            }
        }

        responseObserver.onNext(Books.newBuilder()
                .addAllBooks(matches)
                .setTotalPages(totalPages)
                .build()
        );
        responseObserver.onCompleted();
    }

    private synchronized void loadFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(PERSISTENCE_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Book book = parseBook(line);
                books.put(book.getIsbn(), book);
            }
        } catch (IOException e) {
            // Handle first-run scenario
        }
    }

    private synchronized void persistBook(Book book) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PERSISTENCE_FILE, true))) {
            writer.write(serializeBook(book));
            writer.newLine();
            books.put(book.getIsbn(), book);
        } catch (IOException e) {
            System.err.println("Failed to persist book: " + e.getMessage());
        }
    }

    private String serializeBook(Book book) {
        return String.join("|",
                book.getIsbn(),
                book.getTitle(),
                String.join(",", book.getGenresList()),
                String.valueOf(book.getPageCount())
        );
    }

    private Book parseBook(String line) {
        String[] parts = line.split("\\|");
        return Book.newBuilder()
                .setIsbn(parts[0])
                .setTitle(parts[1])
                .addAllGenres(
                        Arrays.stream(parts[2].split(","))
                                .map(String::toLowerCase)
                                .collect(Collectors.toList())
                )
                .setPageCount(Integer.parseInt(parts[3]))
                .build();
    }
}
