package org.a2aproject.sdk.itk;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting Java ITK Mock Agent...");
        String httpPort = "";
        String grpcPort = "";
        for (int i = 0; i < args.length; i++) {
            if ("--httpPort".equals(args[i]) && i + 1 < args.length) {
                httpPort = args[i + 1];
            } else if ("--grpcPort".equals(args[i]) && i + 1 < args.length) {
                grpcPort = args[i + 1];
            }
        }
        System.out.println("HTTP Port: " + httpPort);
        System.out.println("gRPC Port: " + grpcPort);

        // Verify compilation and import of generated protobuf class
        itk.InstructionOuterClass.Instruction instruction = itk.InstructionOuterClass.Instruction.getDefaultInstance();
        System.out.println("Parsed instructions protobuf class: " + instruction.getClass().getName());

        // Sleep indefinitely so the process remains alive for verification
        Thread.sleep(Long.MAX_VALUE);
    }
}
