package com.tsbonev.nharker.server

import com.tsbonev.nharker.adapter.HelloRepository
import com.tsbonev.nharker.adapter.HelloRepositoryImpl
import com.tsbonev.nharker.adapter.HelloService
import com.tsbonev.nharker.adapter.HelloServiceImpl
import org.koin.dsl.module.module

/**
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */
val helloAppModule = module{
    single<HelloService> {HelloServiceImpl(get())}
    single<HelloRepository> {HelloRepositoryImpl()}
}