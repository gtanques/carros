package com.orange

import io.grpc.Channel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.inject.Singleton
import junit.framework.Assert.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@MicronautTest(transactional = false) // Test de integração
internal class CarrosEndpointTest(
    private val grpcClient: CarrosServiceGrpc.CarrosServiceBlockingStub,
    @Inject val carroRepository: CarroRepository
) {

    /**
     * 1 - caminho feliz
     * 2 - quando já existe carro com a placa
     * 3 - quando os dados de entrada são inválidos
     */


    @BeforeEach
    internal fun setUp() {
        carroRepository.deleteAll()
    }

    @Test
    fun `deve adicionar um novo carro`() {
        // cenário
        val request = CarrosRequest
            .newBuilder()
            .setModelo("Gol")
            .setPlaca("MGG-4080")
            .build()

        // executar ação
        val response = grpcClient.adicionar(request)

        // validação
        with(response) {
            assertNotNull(id)
            assertTrue(carroRepository.existsById(id))
        }

    }

    @Test
    fun `nao deve adicionar novo carro quando a placa ja existe`() {
        // cenário
        val mock: Carro = carroRepository.save(Carro("Gol", "MGG-4080"))

        // executar ação
        val erro = assertThrows<StatusRuntimeException> {
            grpcClient.adicionar(CarrosRequest
                .newBuilder()
                .setModelo("Palio")
                .setPlaca(mock.placa)
                .build())
        }

        println(erro.status.code)
        println(erro.status.description)

        // validação
        with(erro) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("carro com placa existente", status.description)
        }
    }

    @Test
    fun `nao deve adicionar novo carro quando dados de entrada forem invalidos`() {
        // cenário
        val request = CarrosRequest
            .newBuilder()
            .setModelo("")
            .setPlaca("")
            .build()

        // executar ação
        val erro = assertThrows<StatusRuntimeException> {
            grpcClient.adicionar(request)
        }

        // validação
        assertEquals(Status.INVALID_ARGUMENT.code, erro.status.code)
        assertEquals("dados de entrada inválidos", erro.status.description)
    }


    @Factory
    class Clients {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: Channel): CarrosServiceGrpc.CarrosServiceBlockingStub? {
            return CarrosServiceGrpc.newBlockingStub(channel)
        }
    }

}