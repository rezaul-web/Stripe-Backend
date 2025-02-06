package com.example

import com.stripe.Stripe
import com.stripe.model.Customer
import com.stripe.model.EphemeralKey
import com.stripe.model.PaymentIntent
import com.stripe.param.CustomerCreateParams
import com.stripe.param.EphemeralKeyCreateParams
import com.stripe.param.PaymentIntentCreateParams
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Application.configureRouting() {
    Stripe.apiKey = "sk_test_51QnIRoHzOshtNe2jCEJfDYiOa9Tb6OlcBhgpXFGQOQc65lNbd5bAC9WlUnRjsna3kqxW6PHUZXzNLX45XrC1rab600JVtgSSyR"



    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        post("/payment-sheet") {
            try {
                // Parse the request body to extract amount
                val requestBody = call.receive<Map<String, String>>()
                val amount = requestBody["amount"]?.toLongOrNull()?.times(100)
                    ?: throw IllegalArgumentException("Invalid amount provided")

                // Create a new Customer
                val customerParams = CustomerCreateParams.builder().build()
                val customer = Customer.create(customerParams)

                // Create an Ephemeral Key for the customer
                val ephemeralKeyParams = EphemeralKeyCreateParams.builder()
                    .setStripeVersion("2025-01-27.acacia")
                    .setCustomer(customer.id)
                    .build()
                val ephemeralKey = EphemeralKey.create(ephemeralKeyParams)

                // Create a PaymentIntent with dynamic amount
                val paymentIntentParams = PaymentIntentCreateParams.builder()
                    .setAmount(amount) // Use the received amount * 100
                    .setCurrency("inr") // Currency
                    .setCustomer(customer.id) // Associate with the customer
                    .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                            .setEnabled(true)
                            .build()
                    )
                    .build()
                val paymentIntent = PaymentIntent.create(paymentIntentParams)

                // Prepare the response data
                val responseData = mapOf(
                    "paymentIntent" to paymentIntent.clientSecret,
                    "ephemeralKey" to ephemeralKey.secret,
                    "customer" to customer.id,
                    "publishableKey" to "pk_test_51QnIRoHzOshtNe2jhkGcrb2ZW1qY3MLiDI40pHE1dqPYiebOsPE1UiwJds54tOq2hffxN8c3OMKJdrutuR9qzXq1000blo0tWl"
                )

                // Send the response as JSON
                call.respond(HttpStatusCode.OK, responseData)
            } catch (e: Exception) {
                // Handle errors
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "Unknown error")))
            }
        }
    }
}
