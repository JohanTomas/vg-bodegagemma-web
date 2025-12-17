package pe.edu.vallegrande.sistventas.rest;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxExporterConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.sistventas.dto.Reservation;
import pe.edu.vallegrande.sistventas.dto.ReservationDetail;
import pe.edu.vallegrande.sistventas.dto.reports.ReservationReportData;
import pe.edu.vallegrande.sistventas.service.ReservationService;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/reservations")
public class ReservationRest {
    private static final Logger logger = LoggerFactory.getLogger(ReservationRest.class);

    @Autowired
    private ReservationService reservationService;

    // Endpoint to get all reservations
    @GetMapping
    public List<Reservation> getAllReservations() {
        return reservationService.getAllReservations();
    }

    // Endpoint to get a reservation by its ID
    @GetMapping("/{id}")
    public ResponseEntity<Reservation> getReservationById(@PathVariable Long id) {
        Optional<Reservation> reservation = reservationService.getReservationById(id);
        return reservation.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Endpoint to create a new reservation
    @PostMapping
    public Reservation createReservation(@RequestBody Reservation reservation) {
        return reservationService.createReservation(reservation);
    }

    // Endpoint to update a reservation
    @PutMapping("/{id}")
    public ResponseEntity<Reservation> updateReservation(@PathVariable Long id, @RequestBody Reservation reservation) {
        try {
            Reservation updatedReservation = reservationService.updateReservation(id, reservation);
            return ResponseEntity.ok(updatedReservation);
        } catch (ReservationService.ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error updating reservation: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoint to delete a reservation
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        Optional<Reservation> reservation = reservationService.getReservationById(id);
        if (reservation.isPresent()) {
            reservationService.deleteReservation(id);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // New endpoint to get reservations by active status
    @GetMapping("/status/{active}")
    public List<Reservation> getReservationsByActiveStatus(@PathVariable String active) {
        return reservationService.getReservationsByStatus(active);
    }

    // New endpoint to get reservations by active status with pagination
    @GetMapping("/status/{active}/page")
    public Page<Reservation> getReservationsPageableByActiveStatus(@PathVariable String active, Pageable pageable) {
        return reservationService.getReservationsPageableByStatus(active, pageable);
    }

    // Endpoint to logically delete a reservation (change status to 'canceled')
    @PutMapping("/delete/{id}")
    public ResponseEntity<Reservation> logicalDeleteReservation(@PathVariable Long id) {
        try {
            Reservation reservation = reservationService.logicalCancelReservation(id);
            return ResponseEntity.ok(reservation);
        } catch (ReservationService.ResourceConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        } catch (ReservationService.ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Endpoint to logically activate a reservation (change status to 'confirmed')
    @PutMapping("/activate/{id}")
    public ResponseEntity<Reservation> logicalActivateReservation(@PathVariable Long id) {
        try {
            Reservation reservation = reservationService.logicalConfirmReservation(id);
            return ResponseEntity.ok(reservation);
        } catch (ReservationService.ResourceConflictException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        } catch (ReservationService.ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Endpoint to generate PDF report
    @GetMapping("/report/{id}")
    public ResponseEntity<byte[]> generateReservationReport(@PathVariable Long id) {
        try {
            System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();

            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_reservaUnitaria.jasper");

            Optional<Reservation> optionalReservation = this.reservationService.getReservationById(id);

            if (optionalReservation.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Reservation reservation = optionalReservation.get();

            List<ReservationReportData> reservationReportData = new ArrayList<>();
            for (ReservationDetail detail : reservation.getReservationDetails()) {
                ReservationReportData data = new ReservationReportData();
                data.setRESERVATION_ID(BigDecimal.valueOf(reservation.getId()));
                data.setRESERVATION_DATE(reservation.getFormattedDateTime());
                data.setCLIENT_NAME(reservation.getClient().getNames() + " " + reservation.getClient().getLastName());
                data.setSELLER_NAME(reservation.getSeller().getNames() + " " + reservation.getSeller().getLastName());
                data.setPRODUCT_NAME(detail.getProduct().getName());
                data.setUNIT_SALE(detail.getProduct().getUnitSale());
                data.setPAYMENT_METHOD(reservation.getPaymentMethod().getName());
                data.setQUANTITY(BigDecimal.valueOf(detail.getAmount()));
                data.setPRICE_UNIT(BigDecimal.valueOf(detail.getProduct().getPriceUnit()));
                data.setSUBTOTAL_PRODUCT(BigDecimal.valueOf(detail.getSubtotalReservation()));
                data.setTOTAL(BigDecimal.valueOf(reservation.getTotalReservation()));

                reservationReportData.add(data);
            }

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reservationReportData);

            Map<String, Object> parameters = new HashMap<>();
            JasperPrint jasperPrint = JasperFillManager.fillReport(reportStream, parameters, dataSource);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            JasperExportManager.exportReportToPdfStream(jasperPrint, byteArrayOutputStream);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment().filename("reservation_report.pdf").build());

            return new ResponseEntity<>(byteArrayOutputStream.toByteArray(), headers, HttpStatus.OK);

        } catch (JRException | ParserConfigurationException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new byte[0]);
        }
    }

    // Endpoint to generate Excel report
    @GetMapping("/report/excel/{id}")
    public ResponseEntity<byte[]> generateExcelReservationReport(@PathVariable Long id) {
        try {
            System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();

            InputStream reportStream = this.getClass().getResourceAsStream("/reports/report_reservaUnitaria.jasper");

            Optional<Reservation> optionalReservation = this.reservationService.getReservationById(id);

            if (optionalReservation.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Reservation reservation = optionalReservation.get();

            List<ReservationReportData> reservationReportData = new ArrayList<>();
            for (ReservationDetail detail : reservation.getReservationDetails()) {
                ReservationReportData data = new ReservationReportData();
                data.setRESERVATION_ID(BigDecimal.valueOf(reservation.getId()));
                data.setRESERVATION_DATE(reservation.getFormattedDateTime());
                data.setCLIENT_NAME(reservation.getClient().getNames() + " " + reservation.getClient().getLastName());
                data.setSELLER_NAME(reservation.getSeller().getNames() + " " + reservation.getSeller().getLastName());
                data.setPRODUCT_NAME(detail.getProduct().getName());
                data.setUNIT_SALE(detail.getProduct().getUnitSale());
                data.setPAYMENT_METHOD(reservation.getPaymentMethod().getName());
                data.setQUANTITY(BigDecimal.valueOf(detail.getAmount()));
                data.setPRICE_UNIT(BigDecimal.valueOf(detail.getProduct().getPriceUnit()));
                data.setSUBTOTAL_PRODUCT(BigDecimal.valueOf(detail.getSubtotalReservation()));
                data.setTOTAL(BigDecimal.valueOf(reservation.getTotalReservation()));

                reservationReportData.add(data);
            }

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(reservationReportData);

            Map<String, Object> parameters = new HashMap<>();
            JasperPrint jasperPrint = JasperFillManager.fillReport(reportStream, parameters, dataSource);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            JRXlsxExporter exporter = new JRXlsxExporter();
            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(byteArrayOutputStream));
            SimpleXlsxExporterConfiguration configuration = new SimpleXlsxExporterConfiguration();
            exporter.setConfiguration(configuration);
            exporter.exportReport();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment().filename("reservation_report.xlsx").build());

            return new ResponseEntity<>(byteArrayOutputStream.toByteArray(), headers, HttpStatus.OK);

        } catch (JRException | ParserConfigurationException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new byte[0]);
        }
    }
}