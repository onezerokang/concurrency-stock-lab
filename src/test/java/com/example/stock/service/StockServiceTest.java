package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StockServiceTest {
    @Autowired
//    private StockService stockService;
    private PessimisticLockStockService stockService;
    
    @Autowired
    private StockRepository stockRepository;
    
    @BeforeEach
    void before() {
        stockRepository.saveAndFlush(new Stock(1L, 100L));
    }
    
    @AfterEach
    void after() {
        stockRepository.deleteAll();
    }

    @Test
    void 재고감소() {
        // when
        stockService.decrease(1L, 1L);

        //then
        Stock stock = stockRepository.findById(1L).orElseThrow();

        assertEquals(99, stock.getQuantity());
    }

    @Test
    void 동시에_100개의_요청() throws InterruptedException {
        // given
        int threadCount = 100;

        // 비동기로 사용하는 작업을 단순하게 사용할 수 있는 Java의 API
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    stockService.decrease(1L, 1L);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then
        Stock stock = stockRepository.findById(1L).orElseThrow();
        assertEquals(0, stock.getQuantity());
    }

}