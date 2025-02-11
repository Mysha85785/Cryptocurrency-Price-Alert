import java.util.Timer;
import java.util.TimerTask;
import java.util.Scanner;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;

public class CryptoPriceAlert {
    
    private static final String API_URL = "https://api.coingecko.com/api/v3/simple/price?ids=%s&vs_currencies=usd";
    private static String cryptoId;
    private static double thresholdPrice;
    private static String emailRecipient;
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Get the user inputs
        System.out.print("Enter the cryptocurrency (e.g., bitcoin, ethereum): ");
        cryptoId = scanner.nextLine().toLowerCase();

        System.out.print("Enter the price threshold to monitor: ");
        thresholdPrice = scanner.nextDouble();

        scanner.nextLine();  // Consume the newline

        System.out.print("Enter your email address (for alerts): ");
        emailRecipient = scanner.nextLine();

        // Start monitoring the price
        startPriceMonitoring();

        scanner.close();
    }

    // Start a Timer to monitor the price
    private static void startPriceMonitoring() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                double currentPrice = getCryptoPrice(cryptoId);
                System.out.println("Current price of " + cryptoId + ": $" + currentPrice);

                // If price crosses the threshold, send an alert
                if (currentPrice >= thresholdPrice) {
                    sendPriceAlert(currentPrice);
                }
            }
        }, 0, 60000);  // Check every 60 seconds
    }

    // Get the current price of a cryptocurrency from the CoinGecko API
    private static double getCryptoPrice(String cryptoId) {
        try {
            String urlStr = String.format(API_URL, cryptoId);
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            // Read the response
            StringBuilder response = new StringBuilder();
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                Scanner scanner = new Scanner(connection.getInputStream());
                while (scanner.hasNext()) {
                    response.append(scanner.nextLine());
                }
                scanner.close();
                // Parse JSON response to get the price
                JSONObject jsonResponse = new JSONObject(response.toString());
                return jsonResponse.getJSONObject(cryptoId).getDouble("usd");
            } else {
                System.out.println("Failed to get data from the API. Response code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    // Send an email alert when the price crosses the threshold
    private static void sendPriceAlert(double currentPrice) {
        String subject = "Crypto Price Alert: " + cryptoId + " has reached the threshold!";
        String body = String.format("The price of %s has reached $%.2f, which is above your threshold of $%.2f.", cryptoId, currentPrice, thresholdPrice);

        // Email configuration
        String senderEmail = "your-email@gmail.com";  // Change this to your email
        String senderPassword = "your-email-password";  // Change this to your email password (or app-specific password)

        Properties properties = new Properties();
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            // Create the email message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(senderEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailRecipient));
            message.setSubject(subject);
            message.setText(body);

            // Send the email
            Transport.send(message);
            System.out.println("Price alert sent to: " + emailRecipient);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
