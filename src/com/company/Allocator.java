package com.company;


import java.util.*;

public class Allocator {
    private final int size;
    private final int pageSize;
    private byte[] desciptors;
    private byte[] mem;
    private static final int DESCRIPTOR_SIZE = 12;
    private Map<Integer, List<Integer>> hashMap =
            new HashMap<>();
    private int freePagesCount;


    public Allocator(int size, int pageSize) {
        this.size = size;
        this.pageSize = pageSize;
        this.mem = new byte[size];
        this.desciptors = new byte[size / pageSize * DESCRIPTOR_SIZE];
        this.freePagesCount = size/pageSize;
    }


    public void fillDescriptor(int pageIndex, int countOfFreeBlocks, int blockSize) {
        int startIndex = pageIndex == 0 ? 0 : pageIndex * DESCRIPTOR_SIZE;
        int freeBlockIndex = 0;
        if (blockSize >= pageSize) {
            freeBlockIndex = pageIndex * pageSize;
        } else {
            freeBlockIndex = pageIndex * pageSize + blockSize;
        }
        byte[] byteFreeBlockIndex = UtilByte.intInByte(freeBlockIndex);
        byte[] byteCountOfBlocks = UtilByte.intInByte(countOfFreeBlocks - 1);
        byte[] byteBlockSize = UtilByte.intInByte(blockSize);
        System.arraycopy(byteFreeBlockIndex, 0, desciptors, startIndex, 4);
        System.arraycopy(byteCountOfBlocks, 0, desciptors, startIndex + 4, 4);
        System.arraycopy(byteBlockSize, 0, desciptors, startIndex + 8, 4);
    }

    public int mem_alloc(int memory) {
        if (memory > pageSize / 2 && freePagesCount*pageSize>= memory) {
            int firstNotDividedPage = findNotDividedPage();
            for (int itter = 0; itter < memory / pageSize; itter++) {
                int notDividedPage = findNotDividedPage();
                fillDescriptor(notDividedPage, 1, memory);
                addPageIntoMap(memory, notDividedPage);
                freePagesCount--;
            }
            return firstNotDividedPage * pageSize;
        }
        List<Integer> pagesWithSameSize = hashMap.get(memory);
        if (pagesWithSameSize == null || pagesWithSameSize.size() == 0) {
            int notDividedPage = findNotDividedPage();
            if (notDividedPage == -1) {
                return -1;
            }
            dividePageIntoBlocksBySize(notDividedPage, memory);
            fillDescriptor(notDividedPage, pageSize / memory, memory);
            addPageIntoMap(memory, notDividedPage);
            freePagesCount--;
            return notDividedPage * pageSize;
        } else if (pagesWithSameSize != null || pagesWithSameSize.size() != 0) {
            int pageIndex = pagesWithSameSize.get(0);
            return putBlockIntoDividedPage(pageIndex);
        }
        return -1;
    }

    public void mem_free(int index) {
        int page = index / pageSize;    // get page number by index
        byte [] descriptorDelete = {0,0,0,0,0,0,0,0,0,0,0,0};
        byte[] descriptorByPage = Arrays.copyOfRange(desciptors, page * DESCRIPTOR_SIZE, page * DESCRIPTOR_SIZE + DESCRIPTOR_SIZE);
        int freeBlocksOfPage = UtilByte.byteInInt(Arrays.copyOfRange(descriptorByPage, 4, 8));
        int blockSize = UtilByte.byteInInt(Arrays.copyOfRange(descriptorByPage, 8, 12));
         if (freeBlocksOfPage == 0 && blockSize >= pageSize) {
            List<Integer> pagesWhichBlockOccupied = hashMap.get(blockSize);
            for (int itter = 0; itter < pagesWhichBlockOccupied.size(); itter++) {
                int pageNumber = pagesWhichBlockOccupied.get(itter);
                System.arraycopy(descriptorDelete,0,desciptors
                        ,pageNumber*DESCRIPTOR_SIZE,
                        DESCRIPTOR_SIZE);
                freePagesCount++;
            }
            for (int itter = 0; itter <= pagesWhichBlockOccupied.size(); itter++) {
                deletePageFromMap(pagesWhichBlockOccupied.get(0));
            }

        }else if (freeBlocksOfPage == 0) {
            byte[] newEmptyBlockIndex = UtilByte.intInByte(index);
            System.arraycopy(newEmptyBlockIndex, 0, desciptors, page * DESCRIPTOR_SIZE, newEmptyBlockIndex.length);
            List<Integer> ar = new ArrayList<>();
            ar.add(page);
            hashMap.put(blockSize,ar);
            updateBlockCountInDescriptor(page,1);
        }else if(freeBlocksOfPage+1 == pageSize/blockSize){
            hashMap.remove(blockSize);
            System.arraycopy(descriptorDelete,0,desciptors,DESCRIPTOR_SIZE*page , descriptorDelete.length);
            freePagesCount++;
        }
        else {
            byte[] emptyFirstBlockIndex = Arrays.copyOfRange(descriptorByPage, 0, 4);
            byte[] updatedFirstBlockIndex = UtilByte.intInByte(index);
            System.arraycopy(emptyFirstBlockIndex, 0, mem, index, emptyFirstBlockIndex.length);
            System.arraycopy(updatedFirstBlockIndex, 0, desciptors, page * DESCRIPTOR_SIZE, updatedFirstBlockIndex.length);
            updateBlockCountInDescriptor(page,1);
        }
    }

    public int mem_realloc(int size , int indexOfBlock){
        int page = indexOfBlock / pageSize;
        byte [] desciptorOfBlock = Arrays.copyOfRange(desciptors,page*DESCRIPTOR_SIZE,page*DESCRIPTOR_SIZE+DESCRIPTOR_SIZE);
        int blockSize = getSizeOfblockDescriptor(desciptorOfBlock);
        if(blockSize==0){
            return mem_alloc(size);
        }else if(blockSize > pageSize){
            mem_free(indexOfBlock);
            return  mem_alloc(size);
        }else if(blockSize < pageSize){
            mem_free(indexOfBlock);
             return mem_alloc(size);
        }
        return -1;
    }

    public int findNotDividedPage() {
        int startIndex = 8;
        for (int itter = 0; itter < Math.floor(desciptors.length / DESCRIPTOR_SIZE); itter++) {
            byte[] byteArrayOfBlockSize = Arrays.copyOfRange(desciptors, startIndex, startIndex + 4);
            int blockSize = UtilByte.byteInInt(byteArrayOfBlockSize);
            if (blockSize == 0) {
                return itter;
            }
            startIndex += DESCRIPTOR_SIZE;
        }
        return -1;
    }

    private int putBlockIntoDividedPage(int index) {
        byte[] descriptor = Arrays.copyOfRange(desciptors, index * DESCRIPTOR_SIZE, index * DESCRIPTOR_SIZE + DESCRIPTOR_SIZE);
        int indexOfEmptyBlock = UtilByte.byteInInt(Arrays.copyOfRange(descriptor, 0, 4));
        int  count = UtilByte.byteInInt(Arrays.copyOfRange(descriptor, 4, 8));
        if (count == 1) {
            deletePageFromMap(index);
            int num = UtilByte.byteInInt(Arrays.copyOfRange(mem, indexOfEmptyBlock, indexOfEmptyBlock + 4));
            byte[] firstEmptyBlockIndex = UtilByte.intInByte(num);
            System.arraycopy(firstEmptyBlockIndex, 0, desciptors, index * DESCRIPTOR_SIZE, firstEmptyBlockIndex.length);
            updateBlockCountInDescriptor(index,-1);
        } else {
            int num = UtilByte.byteInInt(Arrays.copyOfRange(mem, indexOfEmptyBlock, indexOfEmptyBlock + 4));
            byte[] firstEmptyBlockIndex = UtilByte.intInByte(num);
            updateBlockCountInDescriptor(index,-1);
            System.arraycopy(firstEmptyBlockIndex, 0, desciptors, index * DESCRIPTOR_SIZE, firstEmptyBlockIndex.length);
        }
        return indexOfEmptyBlock;
    }

    public void dividePageIntoBlocksBySize(int indexOfPage, int blockSize) {
        for (int itter = 1; itter <= pageSize / blockSize; itter++) {
            byte[] nextBlock = UtilByte.intInByte((indexOfPage * pageSize) + blockSize * itter);
            System.arraycopy(nextBlock, 0, mem, (indexOfPage * pageSize) + blockSize * (itter - 1), nextBlock.length);
        }
    }

    public byte[] getPagesDescriptor(int pageInd) {
        return Arrays.copyOfRange(desciptors, pageInd * DESCRIPTOR_SIZE, pageInd * DESCRIPTOR_SIZE + DESCRIPTOR_SIZE);
    }

    public int getSizeOfblockDescriptor(byte[] arr) {
        return UtilByte.byteInInt(Arrays.copyOfRange(arr, 8, 12));
    }

    public int getFreeBlockIndexDescriptor(byte[] arr) {
        return UtilByte.byteInInt(Arrays.copyOfRange(arr, 0, 4));
    }

    public int getCountOfFreeBlockDescriptor(byte[] arr) {
        return UtilByte.byteInInt(Arrays.copyOfRange(arr, 4, 8));
    }


    public void updateBlockCountInDescriptor(int page, int val) {
        byte[] decreasedBlockCount = UtilByte.intInByte(getCountOfFreeBlockDescriptor(getPagesDescriptor(page)) + val);
        System.arraycopy(decreasedBlockCount, 0, desciptors, page * DESCRIPTOR_SIZE + 4, 4);
    }

    public void addPageIntoMap(int blockSize, int page) {
        List<Integer> pagesWithSameSize = hashMap.get(blockSize);
        if (pagesWithSameSize == null) {
            pagesWithSameSize = new ArrayList<>();
            pagesWithSameSize.add(page);
            hashMap.put(blockSize, pagesWithSameSize);
        } else {
            if (!pagesWithSameSize.contains(page)) {
                pagesWithSameSize.add(page);
                hashMap.replace(blockSize, pagesWithSameSize);
            }
        }
    }

    public void deletePageFromMap(int page) {
        for (Map.Entry<Integer, List<Integer>> pair : hashMap.entrySet()) {
            pair.getValue().remove((Integer) page);
        }
    }


    public void dump() {

        for (int indexOfPage = 0; indexOfPage < size / pageSize; indexOfPage++) {
            byte[] descriptor = getPagesDescriptor(indexOfPage);
            int blockSize = getSizeOfblockDescriptor(descriptor);
            int count =  getCountOfFreeBlockDescriptor(descriptor);
            int blockIndex = getFreeBlockIndexDescriptor(descriptor);
            String freeBlockIndex =  count==0 && blockSize!=0? "None" : Integer.toString(blockIndex);
            String pageType ;
            if(blockSize==0){
                pageType = "Free";
            }else if(blockSize<=pageSize/2){
                pageType = "Divided";
            }else {
                pageType = "Multiple";
            }
            System.out.println("Page : " + indexOfPage +" Descriptor : " + "block_size : " + blockSize +
                    ", block_counter :" + count +
                    ", first_empty_block : " + freeBlockIndex + " , page_type : " + pageType

            );

            System.out.println();
        }
    }
}
