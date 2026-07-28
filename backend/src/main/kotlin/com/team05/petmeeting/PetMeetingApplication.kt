package com.team05.petmeeting

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@ConfigurationPropertiesScan
@SpringBootApplication
class PetMeetingApplication

fun main(args: Array<String>) {
    runApplication<PetMeetingApplication>(*args)
}
