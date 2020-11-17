package com.company;

public class Main {

    public static void main(String[] args) {
        Allocator allocator = new Allocator(4096,1024);
        allocator.mem_alloc(256);
        allocator.mem_alloc(256);
        allocator.mem_alloc(256);
        allocator.mem_alloc(128);
        allocator.mem_alloc(128);
        allocator.mem_alloc(128);
        allocator.mem_alloc(128);

        allocator.dump();
    }
}
