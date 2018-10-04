package com.tsbonev.nharker.adapter

/**
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */
interface HelloService {
    fun sayHello(): String
}

interface HelloRepository {
    fun getHello(): String
}

class HelloRepositoryImpl : HelloRepository {
    override fun getHello(): String {
        return "Ktor & Koin"
    }
}

class HelloServiceImpl(private val helloRepository: HelloRepository) : HelloService {
    override fun sayHello(): String {
        return helloRepository.getHello()
    }
}