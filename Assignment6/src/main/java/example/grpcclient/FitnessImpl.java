package example.grpcclient; // match Node.java's package

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

import buffers.RequestProtos.Request;
import buffers.RequestProtos.Request.RequestType;
import buffers.ResponseProtos.Response;


class FitnessImpl extends FitnessGrpc.FitnessImplBase {

    // implement Fitness service from .proto
    // rpc addExercise
    // rpc getExercise

    // set of exercises
    private final Map<ExerciseType, List<Exercise>> exercises = new ConcurrentHashMap<>();

    public FitnessImpl() {
        // populate sample exercises
        initializeSampleExercises();
    }

    private void initializeSampleExercises() {
        // sample exercises

        // Cardio Exercises
        addExerciseInternal(Exercise.newBuilder()
                .setDescription("Run 1 mile")
                .setExerciseType(ExerciseType.CARDIO)
                .build()
        );
        addExerciseInternal(Exercise.newBuilder()
                .setDescription("Cycling for 30 minutes")
                .setExerciseType(ExerciseType.CARDIO)
                .build()
        );

        // Strength Exercises
        addExerciseInternal(Exercise.newBuilder()
                .setDescription("Bench Press (3 sets of 10)")
                .setExerciseType(ExerciseType.STRENGTH)
                .build()
        );
        addExerciseInternal(Exercise.newBuilder()
                .setDescription("Deadlifts (4 sets of 8)")
                .setExerciseType(ExerciseType.STRENGTH)
                .build()
        );

        // Balance Exercises
        addExerciseInternal(Exercise.newBuilder()
                .setDescription("Single-leg stand (1 minute)")
                .setExerciseType(ExerciseType.BALANCE)
                .build()
        );
    }

    private void addExerciseInternal(Exercise exercise) {
        exercises.computeIfAbsent(exercise.getExerciseType(), k -> new CopyOnWriteArrayList<>())
                .add(exercise);
    }

    // reading # of exercises client wants , put them in list to return
    @Override
    public void getExercise(GetRequest req, StreamObserver<GetResponse> responseObserver) {
        ExerciseType type = req.getExerciseType();
        List<Exercise> typeExercises = exercises.getOrDefault(type, Collections.emptyList());

        if (typeExercises.isEmpty()) {
            responseObserver.onNext(GetResponse.newBuilder()
                    .setIsSuccess(false)
                    .setError("No " + type.name().toLowerCase() + " exercises found")
                    .build()
            );
        } else {
            Exercise randomExercise = typeExercises.get(new Random().nextInt(typeExercises.size()));
            responseObserver.onNext(GetResponse.newBuilder()
                    .setIsSuccess(true)
                    .setExercise(randomExercise.toBuilder()
                            .setDescription(randomExercise.getDescription())
                            .build())
                    .build()
            );
        }
        responseObserver.onCompleted();
    }

    // take exercise user wants to set and put in set of exercises
    @Override
    public void addExercise(AddRequest req, StreamObserver<AddResponse> responseObserver) {
        Exercise exercise = req.getExercise();

        if (exercise.getDescription().isEmpty()) {
            responseObserver.onNext(AddResponse.newBuilder()
                    .setIsSuccess(false)
                    .setError("Exercise description cannot be empty")
                    .build()
            );
        } else {
            addExerciseInternal(exercise);
            responseObserver.onNext(AddResponse.newBuilder()
                    .setIsSuccess(true)
                    .setError("Exercise added : " + exercise.getDescription() + " ")
                    .build()
            );
        }
        responseObserver.onCompleted();
    }
}
