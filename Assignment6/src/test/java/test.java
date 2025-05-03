import com.google.protobuf.Empty;
import example.grpcclient.Client;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.JSONObject;
import service.*;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class test {
    ManagedChannel channel;
    private FitnessGrpc.FitnessBlockingStub blockingStub1;
    private ZodiacGrpc.ZodiacBlockingStub blockingStub2;
    private LibraryServiceGrpc.LibraryServiceBlockingStub blockingStub3;

    @org.junit.Before
    public void setUp() throws Exception {
        channel = ManagedChannelBuilder.forTarget("localhost:8000").usePlaintext().build();

        blockingStub1 = FitnessGrpc.newBlockingStub(channel);
        blockingStub2 = ZodiacGrpc.newBlockingStub(channel);
        blockingStub3 = LibraryServiceGrpc.newBlockingStub(channel);
    }

    @org.junit.After
    public void close() throws Exception {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    public void FitnessTestFailure() throws IOException {
        // error case
        // test getting non-existent exercise type
        GetRequest invalidGetRequest = GetRequest.newBuilder()
                .setExerciseType(ExerciseType.BALANCE)
                .build();
        GetResponse balanceResponse = blockingStub1.getExercise(invalidGetRequest);
        assertFalse(balanceResponse.getIsSuccess());
        assertEquals("No balance exercises found", balanceResponse.getError());
    }

    @Test
    public void ZodiacTestFailure() throws IOException {
        // error case
        // test invalid date
        SignRequest invalidRequest = SignRequest.newBuilder()
                .setName("Bob")
                .setMonth("Feb")
                .setDay(30)
                .build();
        SignResponse invalidResponse = blockingStub2.sign(invalidRequest);
        assertFalse(invalidResponse.getIsSuccess());
        assertTrue(invalidResponse.getError().contains("Invalid date/month"));
    }

    @Test
    public void LibraryServiceTestFailure() throws IOException {
        // error case
        // test searching non-existent genre
        GenreQuery invalidQuery = GenreQuery.newBuilder().setGenre("Cooking").build();
        Books noResults = blockingStub3.findByGenre(invalidQuery);
        assertEquals(0, noResults.getBooksCount());
    }

    //    @Test
//    public void FitnessTestSuccess() throws IOException {
//        // success case
//        // test adding a valid exercise
//        Exercise exercise = Exercise.newBuilder()
//                .setDescription("Push-ups")
//                .setExerciseType(ExerciseType.STRENGTH)
//                .build();
//        AddRequest addRequest = AddRequest.newBuilder().setExercise(exercise).build();
//        AddResponse addResponse = blockingStub1.addExercise(addRequest);
//        assertTrue(addResponse.getIsSuccess());
//    }

    //    @Test
//    public void ZodiacTestSuccess() throws IOException {
//        // success case
//        // test valid zodiac sign calculation
//        SignRequest validRequest = SignRequest.newBuilder()
//                .setName("Alice")
//                .setMonth("Jul")
//                .setDay(25)
//                .build();
//        SignResponse validResponse = blockingStub2.sign(validRequest);
//        assertTrue(validResponse.getIsSuccess());
//        assertTrue(validResponse.getMessage().contains("Leo"));
//    }

//    @Test
//    public void LibraryServiceTestSuccess() throws IOException {
//        // success case
//        // test adding valid book
//        Book validBook = Book.newBuilder()
//                .setTitle("Effective Java")
//                .addAllGenres(Arrays.asList("Programming", "Education"))
//                .setPageCount(416)
//                .build();
//        BookID addResponse = blockingStub3.addBook(validBook);
//        assertNotNull(addResponse.getId());
//        assertFalse(addResponse.getId().isEmpty());
//    }
}
