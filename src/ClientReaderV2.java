import com.rabbitmq.client.*;

import java.util.*;


public class ClientReaderV2 {
    private static final String REQUEST_EXCHANGE = "RequestLastLine";
    private static final String RESPONSE_EXCHANGE = "ReplicaResponses";
    private static final String RESPONSE_QUEUE = "ResponseQueue";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        List<String> allLines = new ArrayList<>();

        Connection connection = factory.newConnection();
             Channel channel = connection.createChannel();

            // Declare the direct exchange for responses
            channel.exchangeDeclare(RESPONSE_EXCHANGE, BuiltinExchangeType.DIRECT);

            // Declare and bind the queue for responses
            channel.queueDeclare(RESPONSE_QUEUE, false, false, false, null);
            channel.queueBind(RESPONSE_QUEUE, RESPONSE_EXCHANGE, RESPONSE_QUEUE);

            // Set up consumer to listen on the response queue
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String messageReceived = new String(delivery.getBody(), "UTF-8");
                Collections.addAll(allLines, messageReceived.split("\n"));
            };



            // Declare the fanout exchange for requests and publish a request
            channel.exchangeDeclare(REQUEST_EXCHANGE, BuiltinExchangeType.FANOUT);
            String message = "READALL";
            channel.basicPublish(REQUEST_EXCHANGE, "", null, message.getBytes("UTF-8"));
            System.out.println(" [x] Sent request '" + message + "'");
        channel.basicConsume(RESPONSE_QUEUE, true, deliverCallback, consumerTag -> {});
        System.out.println("Press Enter to process data...");
        new Scanner(System.in).nextLine();
        processLines(allLines);


    }
    private static void processLines(List<String> lines) {
        Map<String, Integer> lineCounts = new HashMap<>();
        int maxOccurrence = 0;

        // Count occurrences of each line
        for (String line : lines) {
            int count = lineCounts.getOrDefault(line, 0) + 1;
            lineCounts.put(line, count);
            maxOccurrence = Math.max(maxOccurrence, count);
        }

        // Display lines that have the maximum occurrence
        for (Map.Entry<String, Integer> entry : lineCounts.entrySet()) {
            if (entry.getValue() > maxOccurrence/2) {
                System.out.println("Recieved: " + entry.getKey());
            }
        }
    }
}
