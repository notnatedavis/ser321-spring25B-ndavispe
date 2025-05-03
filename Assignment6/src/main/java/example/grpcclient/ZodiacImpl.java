package example.grpcclient;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerMethodDefinition;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import service.*; // imports zodiac.proto
import java.util.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.util.ArrayList;

import buffers.RequestProtos.Request;
import buffers.RequestProtos.Request.RequestType;
import buffers.ResponseProtos.Response;

public class ZodiacImpl extends ZodiacGrpc.ZodiacImplBase {

    // implement Zodiac service from .proto
    // rpc sign
    // rpc find

    // set of zodiacs
    private final List<ZodiacEntry> entries = new ArrayList<>();
    private final ZodiacCalculator zodiacCalculator = new ZodiacCalculator();

    public ZodiacImpl() {
        // populate sample entries
        entries.add(ZodiacEntry.newBuilder()
                .setName("John Doe")
                .setMonth("Jul") // 3 letter abbreviation
                .setDay(25)
                .setSign("Leo")
                .build()
        );
    }

    // reading # of zodiacs client wants , put them in list to return
    @Override
    public void find(FindRequest req, StreamObserver<FindResponse> responseObserver) {
        List<ZodiacEntry> matches = new ArrayList<>();
        String targetSign = req.getSign().trim().toLowerCase();
        FindResponse.Builder responseBuilder = FindResponse.newBuilder();

        for (ZodiacEntry entry : entries) {
            if (entry.getSign().equalsIgnoreCase(targetSign)) {
                // Format entry with emojis
                ZodiacEntry formattedEntry = entry.toBuilder()
                        .setName(entry.getName())
                        .setSign(entry.getSign())
                        .build();
                matches.add(formattedEntry);
            }
        }

        if (matches.isEmpty()) {
            responseBuilder
                    .setIsSuccess(false)
                    .setError("No users found with zodiac sign : " + req.getSign());
        } else {
            responseBuilder
                    .setIsSuccess(true)
                    .addAllEntries(matches)
                    .setError("Found " + matches.size() + " users with sign " + zodiacCalculator.capitalize(targetSign));
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    // take zodiac user wants to set and put in set of zodiacs
    @Override
    public void sign(SignRequest req, StreamObserver<SignResponse> responseObserver) {
        String sign = zodiacCalculator.calculateSign(req.getMonth(), req.getDay());
        SignResponse.Builder response = SignResponse.newBuilder();

        if (sign == null) {
            response
                    .setIsSuccess(false)
                    .setError("Invalid date/month combination for " + req.getMonth() + " " + req.getDay());
        } else {
            // create formatted entry
            ZodiacEntry entry = ZodiacEntry.newBuilder()
                    .setName(req.getName())
                    .setMonth(req.getMonth())
                    .setDay(req.getDay())
                    .setSign(sign)
                    .build();

            entries.add(entry);

            // Build formatted response
            String formattedMessage = String.format(
                    "\n  %s \n  Birthday : %s %d\n%s\n",
                    entry.getSign(),
                    entry.getMonth(),
                    entry.getDay(),
                    zodiacCalculator.getTraitsMessage(sign)
            );

            response
                    .setIsSuccess(true)
                    .setMessage(formattedMessage);
        }

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    // helper class to calculate Zodiac signs
    private static class ZodiacCalculator {
        private static final Map<String, List<Integer>> SIGN_DATES = Map.ofEntries(
                Map.entry("aries", Arrays.asList(3, 21, 4, 19)),         // Mar 21 - Apr 19
                Map.entry("taurus", Arrays.asList(4, 20, 5, 20)),        // Apr 20 - May 20
                Map.entry("gemini", Arrays.asList(5, 21, 6, 20)),        // May 21 - June 20
                Map.entry("cancer", Arrays.asList(6, 21, 7, 22)),        // June 21 - July 22
                Map.entry("leo", Arrays.asList(7, 23, 8, 22)),           // July 23 - August 22
                Map.entry("virgo", Arrays.asList(8, 23, 9, 22)),         // August 23 - September 22
                Map.entry("libra", Arrays.asList(9, 23, 10, 22)),        // September 23 - October 22
                Map.entry("scorpio", Arrays.asList(10, 23, 11, 21)),     // October 23 - November 21
                Map.entry("sagittarius", Arrays.asList(11, 22, 12, 21)), // November 22 - December 21
                Map.entry("capricorn", Arrays.asList(12, 22, 1, 19)),    // December 22 - January 19
                Map.entry("aquarius", Arrays.asList(1, 20, 2, 18)),      // January 20 - February 18
                Map.entry("pisces", Arrays.asList(2, 19, 3, 20))         // February 19 - March 20
        );

        private static final Map<String, String> TRAITS = Map.ofEntries(
                Map.entry("aries", "Leader, competitive, bold ambitious"),
                Map.entry("taurus", "Relaxed, serene, soft"),
                Map.entry("gemini", "Spontaneous, playful, erratic, curious"),
                Map.entry("cancer", "Intuitive, protective, reserved"),
                Map.entry("leo", "Vivacious, theatrical, fiery"),
                Map.entry("virgo", "Logical, practical, systematic"),
                Map.entry("libra", "Symmetry, chases balance"),
                Map.entry("scorpio", "Elusive, mysterious, courageous"),
                Map.entry("sagittarius", "Driven, focused, educated"),
                Map.entry("capricorn", "Patient, dedicated, focused"),
                Map.entry("aquarius", "Innovative, progressive, revolutionary"),
                Map.entry("pisces", "Intuitive, sensitive, empathetic")
        );

        public String calculateSign(String month, int day) {
            // convert month to numerical value (e.g., "Jan" → 1)
            int monthNum = parseMonth(month);
            if (monthNum == -1) return null;

            for (Map.Entry<String, List<Integer>> entry : SIGN_DATES.entrySet()) {
                List<Integer> dates = entry.getValue();
                if ((monthNum == dates.get(0) && day >= dates.get(1)) ||
                        (monthNum == dates.get(2) && day <= dates.get(3))) {
                    return capitalize(entry.getKey());
                }
            }
            return null;
        }

        public String getTraitsMessage(String sign) {
            String lowerSign = sign.toLowerCase();
            String traits = TRAITS.getOrDefault(lowerSign, "No traits found");
            return "\nPersonality Traits :\n" +
                    String.join("\n", traits.split(", ")) +
                    "\n\nDate Range : " + getDateRange(lowerSign);
        }

        private int parseMonth(String month) {
            return switch (month.toLowerCase()) {
                case "jan" -> 1;
                case "feb" -> 2;
                case "mar" -> 3;
                case "apr" -> 4;
                case "may" -> 5;
                case "jun" -> 6;
                case "jul" -> 7;
                case "aug" -> 8;
                case "sep" -> 9;
                case "oct" -> 10;
                case "nov" -> 11;
                case "dec" -> 12;
                default -> -1;
            };
        }

        private String getDateRange(String sign) {
            // proper error handling ?
            List<Integer> dates = SIGN_DATES.getOrDefault(sign, Arrays.asList(0, 0, 0, 0)); // if null could crash
            return String.format("%s %d - %s %d",
                    monthToString(dates.get(0)), dates.get(1),
                    monthToString(dates.get(2)), dates.get(3)
            );
        }

        private String monthToString(int month) {
            return switch (month) {
                case 1 -> "Jan";
                case 2 -> "Feb";
                case 3 -> "Mar";
                case 4 -> "Apr";
                case 5 -> "May";
                case 6 -> "Jun";
                case 7 -> "Jul";
                case 8 -> "Aug";
                case 9 -> "Sep";
                case 10 -> "Oct";
                case 11 -> "Nov";
                case 12 -> "Dec";
                default -> "Invalid";
            };
        }

        private String capitalize(String s) {
            return s.substring(0, 1).toUpperCase() + s.substring(1);
        }
    }
}
