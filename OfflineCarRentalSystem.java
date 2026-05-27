
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

public class OfflineCarRentalSystem {

    // 1. JAVA OOP MODEL CLASSES
    public static abstract class Vehicle {
        protected int id;
        protected String modelName;
        protected String category;
        protected double ratePerDay;

        public Vehicle(int id, String modelName, String category, double ratePerDay) {
            this.id = id; this.modelName = modelName; this.category = category; this.ratePerDay = ratePerDay;
        }
        public abstract double calculateRentalCost(int days);
        public int getId() { return id; }
        public String getModelName() { return modelName; }
        public String getCategory() { return category; }
        public double getRatePerDay() { return ratePerDay; }
    }

    public static class Car extends Vehicle {
        private String transmission;
        private int seats;
        private String fuelType;
        private String imageUrl;

        public Car(int id, String modelName, String category, double ratePerDay, String transmission, int seats, String fuelType, String imageUrl) {
            super(id, modelName, category, ratePerDay);
            this.transmission = transmission; this.seats = seats; this.fuelType = fuelType; this.imageUrl = imageUrl;
        }
        @Override
        public double calculateRentalCost(int days) { return (this.ratePerDay * days) * 1.10; } // 10% tax
        public String getTransmission() { return transmission; }
        public int getSeats() { return seats; }
        public String getFuelType() { return fuelType; }
        public String getImageUrl() { return imageUrl; }
    }

    // 2. JDBC DATA ACCESS OBJECT (DAO)

    public static class CarRentalDAO {
        private static final String DB_URL = "jdbc:mysql://localhost:3306/apexdrive_db";
        private static final String USER = "root";
        private static final String PASS = "password"; // Update if needed

        private Connection getConnection() throws SQLException {
            return DriverManager.getConnection(DB_URL, USER, PASS);
        }

        public List<Car> getAllCars() {
            List<Car> cars = new ArrayList<>();
            String query = "SELECT * FROM cars WHERE is_available = true";
            try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query); ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    cars.add(new Car(rs.getInt("car_id"), rs.getString("model_name"), rs.getString("category"), rs.getDouble("rate_per_day"), rs.getString("transmission"), rs.getInt("seats"), rs.getString("fuel_type"), rs.getString("image_url")));
                }
            } catch (SQLException e) { System.err.println("DB Error: " + e.getMessage()); }
            return cars;
        }

        public boolean bookCar(String name, String license, String phone, int carId, int days, double totalAmount) {
            String insertCustomer = "INSERT INTO customers (full_name, license_number, phone_number) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE full_name=?, phone_number=?";
            String getCustId = "SELECT customer_id FROM customers WHERE license_number = ?";
            String insertRental = "INSERT INTO rentals (customer_id, car_id, rental_days, total_amount) VALUES (?, ?, ?, ?)";
            String updateCar = "UPDATE cars SET is_available = false WHERE car_id = ?";
            
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false);
                try {
                    try (PreparedStatement cStmt = conn.prepareStatement(insertCustomer)) {
                        cStmt.setString(1, name); cStmt.setString(2, license); cStmt.setString(3, phone); cStmt.setString(4, name); cStmt.setString(5, phone); cStmt.executeUpdate();
                    }
                    int custId = -1;
                    try (PreparedStatement gStmt = conn.prepareStatement(getCustId)) {
                        gStmt.setString(1, license); ResultSet rs = gStmt.executeQuery(); if (rs.next()) custId = rs.getInt("customer_id");
                    }
                    if (custId == -1) throw new SQLException("Customer ID error.");
                    try (PreparedStatement rStmt = conn.prepareStatement(insertRental)) {
                        rStmt.setInt(1, custId); rStmt.setInt(2, carId); rStmt.setInt(3, days); rStmt.setDouble(4, totalAmount); rStmt.executeUpdate();
                    }
                    try (PreparedStatement uStmt = conn.prepareStatement(updateCar)) {
                        uStmt.setInt(1, carId); uStmt.executeUpdate();
                    }
                    conn.commit(); return true;
                } catch (SQLException ex) { conn.rollback(); return false; }
            } catch (SQLException e) { return false; }
        }

        public boolean addCar(Car car) {
            String query = "INSERT INTO cars (model_name, category, rate_per_day, transmission, seats, fuel_type, image_url, is_available) VALUES (?, ?, ?, ?, ?, ?, ?, true)";
            try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, car.getModelName()); stmt.setString(2, car.getCategory()); stmt.setDouble(3, car.getRatePerDay()); stmt.setString(4, car.getTransmission()); stmt.setInt(5, car.getSeats()); stmt.setString(6, car.getFuelType()); stmt.setString(7, car.getImageUrl());
                return stmt.executeUpdate() > 0;
            } catch (SQLException e) { return false; }
        }
    }

    // 3. EMBEDDED HTTP WEB SERVER & HTML

    private static final CarRentalDAO dao = new CarRentalDAO();

    public static void main(String[] args) throws IOException {
        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new HomeHandler());
        server.createContext("/book", new BookingHandler());
        server.createContext("/admin/add", new AddCarHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("=======================================================");
        System.out.println("🚀 ApexDrive All-In-One Offline Server started!");
        System.out.println("👉 Open your browser: http://localhost:" + port);
        System.out.println("=======================================================");
    }

    static class HomeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) { exchange.sendResponseHeaders(405, -1); return; }
            List<Car> cars = dao.getAllCars();
            String grid = buildRoyalGrid(cars);
            sendHtmlResponse(exchange, getHtml(grid, ""));
        }
    }

    static class BookingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                Map<String, String> params = parseParams(exchange);
                String name = params.get("customerName"); String license = params.get("license"); String phone = params.get("phone");
                int carId = Integer.parseInt(params.get("carId")); String carModel = params.get("carModel");
                int days = Integer.parseInt(params.get("days")); double price = Double.parseDouble(params.get("price"));
                double total = (price * days) * 1.10;
                boolean success = dao.bookCar(name, license, phone, carId, days, total);
                
                String alert = success ? "<div class='bg-green-900/30 border border-green-700 text-green-300 p-5 rounded-2xl mb-12 font-semibold flex items-center gap-3 shadow-lg max-w-4xl mx-auto'><i class='fa-solid fa-circle-check text-2xl text-green-400'></i><span>SUCCESS: Booking confirmed for <b>" + name + "</b> (" + carModel + "). Total: $" + String.format("%.2f", total) + "</span></div>"
                                       : "<div class='bg-red-900/30 border border-red-700 text-red-300 p-5 rounded-2xl mb-12 font-semibold flex items-center gap-3 shadow-lg max-w-4xl mx-auto'><i class='fa-solid fa-triangle-exclamation text-2xl text-red-400'></i><span>ERROR: Booking failed. Check MySQL connection.</span></div>";
                
                List<Car> cars = dao.getAllCars();
                String grid = buildRoyalGrid(cars);
                sendHtmlResponse(exchange, getHtml(grid, alert));
            }
        }
    }


    private static String buildRoyalGrid(List<Car> cars) {
        StringBuilder grid = new StringBuilder();
        for (Car car : cars) {
            grid.append("<div class='bg-[#0b1020] border border-blue-900/30 rounded-2xl overflow-hidden hover:border-blue-600/50 transition-all group flex flex-col'>")
                .append("<div class='relative h-56 overflow-hidden'><img src='").append(car.getImageUrl()).append("' class='w-full h-full object-cover group-hover:scale-110 transition-transform duration-700'>")
                .append("<div class='absolute top-4 left-4 bg-[#05070f]/90 border border-blue-900/50 px-3 py-1 text-[10px] font-bold text-blue-400 uppercase tracking-widest rounded'>").append(car.getCategory()).append("</div>")
                .append("<div class='absolute bottom-4 right-4 royal-gradient px-4 py-2 rounded-lg text-white font-bold text-lg shadow-lg'>$").append(car.getRatePerDay()).append("<span class='text-xs font-normal opacity-80'>/day</span></div></div>")
                .append("<div class='p-6 flex-1 flex flex-col'>")
                .append("<h3 class='royal-font text-2xl font-bold text-white mb-3'>").append(car.getModelName()).append("</h3>")
                .append("<div class='flex gap-4 text-xs text-slate-400 mb-6 pb-6 border-b border-blue-900/30'>")
                .append("<span><i class='fa-solid fa-gear text-blue-400 mr-1'></i> ").append(car.getTransmission()).append("</span>")
                .append("<span><i class='fa-solid fa-user text-blue-400 mr-1'></i> ").append(car.getSeats()).append(" Seats</span>")
                .append("<span><i class='fa-solid fa-gas-pump text-blue-400 mr-1'></i> ").append(car.getFuelType()).append("</span></div>")
                .append("<form action='/book' method='POST' class='space-y-3 mt-auto'>")
                .append("<input type='hidden' name='carId' value='").append(car.getId()).append("'>")
                .append("<input type='hidden' name='carModel' value='").append(car.getModelName()).append("'>")
                .append("<input type='hidden' name='price' value='").append(car.getRatePerDay()).append("'>")
                .append("<input type='text' name='customerName' required placeholder='Full Name' class='w-full bg-[#05070f] border border-blue-900/40 rounded-lg px-4 py-2.5 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-600'>")
                .append("<input type='text' name='license' required placeholder='Driver License #' class='w-full bg-[#05070f] border border-blue-900/40 rounded-lg px-4 py-2.5 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-600'>")
                .append("<input type='tel' name='phone' required placeholder='Phone Number' class='w-full bg-[#05070f] border border-blue-900/40 rounded-lg px-4 py-2.5 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-blue-600'>")
                .append("<input type='number' name='days' min='1' max='30' value='3' class='w-full bg-[#05070f] border border-blue-900/40 rounded-lg px-4 py-2.5 text-sm text-white focus:outline-none focus:border-blue-600'>")
                .append("<button type='submit' class='w-full royal-gradient text-white font-bold py-3 rounded-lg shadow-lg royal-glow text-xs tracking-widest uppercase'><i class='fa-solid fa-calendar-check mr-1'></i> Reserve Now</button>")
                .append("</form></div></div>");
        }
        return grid.toString();
    }

    static class AddCarHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                Map<String, String> params = parseParams(exchange);
                Car car = new Car(0, params.get("modelName"), params.get("category"), Double.parseDouble(params.get("ratePerDay")), params.get("transmission"), Integer.parseInt(params.get("seats")), params.get("fuelType"), params.get("imageUrl"));
                dao.addCar(car);
                exchange.getResponseHeaders().set("Location", "/"); exchange.sendResponseHeaders(303, -1);
            }
        }
    }

    private static Map<String, String> parseParams(HttpExchange exchange) throws IOException {
        Map<String, String> map = new HashMap<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8));
        String query = br.readLine();
        if (query != null) {
            for (String pair : query.split("&")) {
                String[] p = pair.split("=");
                map.put(URLDecoder.decode(p[0], StandardCharsets.UTF_8), p.length > 1 ? URLDecoder.decode(p[1], StandardCharsets.UTF_8) : "");
            }
        }
        return map;
    }

    private static void sendHtmlResponse(HttpExchange exchange, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        OutputStream os = exchange.getResponseBody(); os.write(bytes); os.close();
    }

    private static String getHtml(String grid, String alert) {
        return "<!DOCTYPE html><html lang='en' class='scroll-smooth'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'><title>ApexDrive | Royal Car Rentals</title><script src='https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4'></script><link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css'><link href='https://fonts.googleapis.com/css2?family=Cormorant+Garamond:wght@500;600;700&family=Plus+Jakarta+Sans:wght@300;400;500;600;700;800&display=swap' rel='stylesheet'><style>body{font-family:'Plus Jakarta Sans',sans-serif;background-color:#05070f;color:#e2e8f0;}.royal-font{font-family:'Cormorant Garamond',serif;}.royal-gradient{background:linear-gradient(135deg,#1d4ed8 0%,#4169E1 50%,#1e3a8a 100%);}.royal-glow{box-shadow:0 0 30px rgba(29,78,216,0.35);}.gold-line{background:linear-gradient(90deg,transparent,#c9a961,transparent);}.bg-hero-royal{background-color:#05070f;background-image:radial-gradient(circle at 20% 20%,rgba(29,78,216,0.15) 0%,transparent 50%),radial-gradient(circle at 80% 60%,rgba(65,105,225,0.1) 0%,transparent 50%);}</style></head><body class='min-h-screen'>" +
               "<header class='fixed top-0 left-0 right-0 z-50 bg-[#05070f]/90 backdrop-blur-xl border-b border-blue-900/30'><div class='max-w-7xl mx-auto px-6 lg:px-12 h-20 flex items-center justify-between'><a href='#home' class='flex items-center gap-3'><div class='w-11 h-11 rounded-lg royal-gradient flex items-center justify-center shadow-lg royal-glow'><i class='fa-solid fa-crown text-white text-lg'></i></div><div class='leading-none'><div class='text-xl font-extrabold text-white'>ApexDrive</div><div class='text-[9px] tracking-[0.3em] text-blue-400 uppercase font-semibold'>Royal Rentals</div></div></a><nav class='hidden md:flex items-center gap-1'><a href='#home' class='px-6 py-2.5 text-sm font-semibold tracking-wider uppercase text-white hover:text-blue-400'>Home</a><a href='#about' class='px-6 py-2.5 text-sm font-semibold tracking-wider uppercase text-slate-400 hover:text-blue-400'>About Us</a><a href='#booking' class='px-6 py-2.5 text-sm font-semibold tracking-wider uppercase text-slate-400 hover:text-blue-400'>Booking</a><a href='#contact' class='px-6 py-2.5 text-sm font-semibold tracking-wider uppercase text-slate-400 hover:text-blue-400'>Contact</a></nav><a href='#booking' class='hidden md:inline-flex royal-gradient text-white font-bold px-6 py-2.5 rounded-lg shadow-lg royal-glow text-sm tracking-wider uppercase'>Reserve Now</a></div></header>" +
               "<section id='home' class='relative min-h-screen bg-hero-royal pt-32 pb-20 flex items-center overflow-hidden'><div class='max-w-7xl mx-auto px-6 lg:px-12 grid lg:grid-cols-2 gap-12 items-center relative z-10 w-full'><div class='space-y-8'><div class='flex items-center gap-3'><div class='w-12 h-[1px] gold-line'></div><span class='text-xs tracking-[0.4em] text-amber-400 uppercase font-semibold'>Premium Car Rental</span></div><h1 class='royal-font text-6xl lg:text-8xl font-bold leading-[0.95] text-white'>Drive in <span class='block italic text-blue-400'>Royalty.</span></h1><p class='text-lg text-slate-400 leading-relaxed max-w-lg font-light'>Experience the pinnacle of luxury driving. Our curated fleet delivers unmatched elegance and sophistication.</p><div class='flex flex-wrap gap-4 pt-4'><a href='#booking' class='royal-gradient text-white font-bold px-8 py-4 rounded-lg shadow-xl royal-glow text-sm tracking-widest uppercase'><i class='fa-solid fa-calendar-check mr-2'></i> Book Now</a><a href='#about' class='bg-transparent border-2 border-blue-800 text-blue-300 hover:bg-blue-900/30 font-bold px-8 py-4 rounded-lg text-sm tracking-widest uppercase'>Discover More</a></div></div><div class='relative'><div class='absolute -inset-4 royal-gradient rounded-3xl blur-2xl opacity-30'></div><div class='relative bg-[#0b1020] rounded-3xl p-3 border border-blue-900/50 shadow-2xl'><img src='https://images.unsplash.com/photo-1555215695-3004980ad54e?auto=format&fit=crop&w=1200&q=80' class='rounded-2xl w-full h-[500px] object-cover'></div></div></div></section>" +
               "<section id='about' class='py-24 bg-[#0a0e27] border-y border-blue-900/20'><div class='max-w-7xl mx-auto px-6 lg:px-12'><div class='text-center max-w-3xl mx-auto mb-16 space-y-4'><div class='flex items-center justify-center gap-3'><div class='w-12 h-[1px] gold-line'></div><span class='text-xs tracking-[0.4em] text-amber-400 uppercase font-semibold'>About Us</span><div class='w-12 h-[1px] gold-line'></div></div><h2 class='royal-font text-5xl font-bold text-white'>The Art of <span class='italic text-blue-400'>Premium</span> Mobility</h2><p class='text-slate-400 leading-relaxed font-light'>For over a decade, ApexDrive has redefined the car rental experience with uncompromising luxury and meticulous service.</p></div><div class='grid md:grid-cols-3 gap-8'><div class='bg-[#0b1020] border border-blue-900/30 rounded-2xl p-8'><div class='w-14 h-14 rounded-xl royal-gradient flex items-center justify-center text-white text-xl mb-6 shadow-lg royal-glow'><i class='fa-solid fa-gem'></i></div><h3 class='royal-font text-2xl font-bold text-white mb-3'>Curated Luxury</h3><p class='text-slate-400 text-sm leading-relaxed font-light'>Every vehicle is hand-selected and meticulously maintained to ensure a royal experience.</p></div><div class='bg-[#0b1020] border border-blue-900/30 rounded-2xl p-8'><div class='w-14 h-14 rounded-xl royal-gradient flex items-center justify-center text-white text-xl mb-6 shadow-lg royal-glow'><i class='fa-solid fa-shield-halved'></i></div><h3 class='royal-font text-2xl font-bold text-white mb-3'>Total Peace of Mind</h3><p class='text-slate-400 text-sm leading-relaxed font-light'>Comprehensive insurance, 24/7 roadside assistance, and transparent pricing.</p></div><div class='bg-[#0b1020] border border-blue-900/30 rounded-2xl p-8'><div class='w-14 h-14 rounded-xl royal-gradient flex items-center justify-center text-white text-xl mb-6 shadow-lg royal-glow'><i class='fa-solid fa-concierge-bell'></i></div><h3 class='royal-font text-2xl font-bold text-white mb-3'>Concierge Service</h3><p class='text-slate-400 text-sm leading-relaxed font-light'>From airport pickup to personalized itineraries, our team crafts unforgettable experiences.</p></div></div></div></section>" +
               "<section id='booking' class='py-24 bg-[#05070f]'><div class='max-w-7xl mx-auto px-6 lg:px-12'><div class='text-center max-w-3xl mx-auto mb-16 space-y-4'><div class='flex items-center justify-center gap-3'><div class='w-12 h-[1px] gold-line'></div><span class='text-xs tracking-[0.4em] text-amber-400 uppercase font-semibold'>Booking</span><div class='w-12 h-[1px] gold-line'></div></div><h2 class='royal-font text-5xl font-bold text-white'>Select Your <span class='italic text-blue-400'>Chariot</span></h2><p class='text-slate-400 font-light'>Choose from our distinguished fleet and reserve your vehicle below.</p></div>" + alert + "<div class='grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8'>" + grid + "</div>" +
               "<div id='admin' class='bg-[#0b1020] border border-blue-900/30 rounded-2xl p-8 mt-20'><div class='mb-8 text-center'><span class='text-xs tracking-[0.4em] text-amber-400 uppercase font-semibold'>Admin Panel</span><h3 class='royal-font text-3xl font-bold text-white mt-2'>Add New Vehicle</h3></div><form action='/admin/add' method='POST' class='grid grid-cols-1 sm:grid-cols-2 gap-4 max-w-4xl mx-auto'><input type='text' name='modelName' required placeholder='Model Name' class='bg-[#05070f] border border-blue-900/40 rounded-lg p-3 text-sm text-white'><select name='category' class='bg-[#05070f] border border-blue-900/40 rounded-lg p-3 text-sm text-white'><option value='Luxury'>Luxury</option><option value='Sports'>Sports</option><option value='Electric'>Electric</option><option value='SUV'>SUV</option></select><input type='number' name='ratePerDay' required placeholder='Rate Per Day' class='bg-[#05070f] border border-blue-900/40 rounded-lg p-3 text-sm text-white'><select name='transmission' class='bg-[#05070f] border border-blue-900/40 rounded-lg p-3 text-sm text-white'><option value='Automatic'>Automatic</option><option value='Manual'>Manual</option></select><input type='number' name='seats' required value='4' class='bg-[#05070f] border border-blue-900/40 rounded-lg p-3 text-sm text-white'><input type='text' name='fuelType' required placeholder='Fuel Type' class='bg-[#05070f] border border-blue-900/40 rounded-lg p-3 text-sm text-white'><input type='url' name='imageUrl' required placeholder='Image URL' value='https://images.unsplash.com/photo-1503376780353-7e6692767b70?auto=format&fit=crop&w=800&q=80' class='sm:col-span-2 bg-[#05070f] border border-blue-900/40 rounded-lg p-3 text-sm text-white'><button type='submit' class='sm:col-span-2 royal-gradient text-white font-bold py-4 rounded-lg shadow-lg royal-glow text-sm tracking-widest uppercase'>Add Vehicle to Database</button></form></div>" +
               "</div></section>" +
               "<section id='contact' class='py-24 bg-[#0a0e27] border-t border-blue-900/20'><div class='max-w-7xl mx-auto px-6 lg:px-12'><div class='text-center max-w-3xl mx-auto mb-16 space-y-4'><div class='flex items-center justify-center gap-3'><div class='w-12 h-[1px] gold-line'></div><span class='text-xs tracking-[0.4em] text-amber-400 uppercase font-semibold'>Contact</span><div class='w-12 h-[1px] gold-line'></div></div><h2 class='royal-font text-5xl font-bold text-white'>Get in <span class='italic text-blue-400'>Touch</span></h2><p class='text-slate-400 font-light'>Our concierge team is ready to assist you.</p></div><div class='grid md:grid-cols-2 gap-6 max-w-4xl mx-auto'><div class='bg-[#0b1020] border border-blue-900/30 rounded-2xl p-6 flex items-start gap-5'><div class='w-12 h-12 rounded-lg royal-gradient flex items-center justify-center text-white flex-shrink-0 shadow-lg royal-glow'><i class='fa-solid fa-location-dot'></i></div><div><h4 class='text-white font-bold mb-1'>Visit Our Showroom</h4><p class='text-slate-400 text-sm'>125 Royal Avenue, Manhattan<br>New York, NY 10001</p></div></div><div class='bg-[#0b1020] border border-blue-900/30 rounded-2xl p-6 flex items-start gap-5'><div class='w-12 h-12 rounded-lg royal-gradient flex items-center justify-center text-white flex-shrink-0 shadow-lg royal-glow'><i class='fa-solid fa-phone'></i></div><div><h4 class='text-white font-bold mb-1'>Call Our Concierge</h4><p class='text-slate-400 text-sm'>+1 (555) 123-4567<br>Available 24/7</p></div></div><div class='bg-[#0b1020] border border-blue-900/30 rounded-2xl p-6 flex items-start gap-5'><div class='w-12 h-12 rounded-lg royal-gradient flex items-center justify-center text-white flex-shrink-0 shadow-lg royal-glow'><i class='fa-solid fa-envelope'></i></div><div><h4 class='text-white font-bold mb-1'>Email Us</h4><p class='text-slate-400 text-sm'>concierge@apexdrive.com</p></div></div><div class='bg-[#0b1020] border border-blue-900/30 rounded-2xl p-6 flex items-start gap-5'><div class='w-12 h-12 rounded-lg royal-gradient flex items-center justify-center text-white flex-shrink-0 shadow-lg royal-glow'><i class='fa-solid fa-clock'></i></div><div><h4 class='text-white font-bold mb-1'>Working Hours</h4><p class='text-slate-400 text-sm'>24 Hours / 7 Days</p></div></div></div></div></section>" +
               "<footer class='bg-[#05070f] border-t border-blue-900/30 py-10'><div class='max-w-7xl mx-auto px-6 lg:px-12 text-center text-xs text-slate-500'>&copy; 2026 ApexDrive Royal Rentals. All rights reserved.</div></footer></body></html>";
    }
}
