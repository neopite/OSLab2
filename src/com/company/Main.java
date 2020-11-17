package com.company;

public class Main {

    public static void main(String[] args) {
        Allocator allocator = new Allocator(4096,1024);
        allocator.mem_alloc(123);
        allocator.mem_alloc(120);
        allocator.mem_alloc(243);
        allocator.mem_alloc(243);
        allocator.mem_alloc(243);
        allocator.mem_alloc(1042);
        allocator.dump();
    }
}
