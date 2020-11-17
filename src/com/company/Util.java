package com.company;

public class Util {
    public static int findNearestNumberOfPow2(int num) {
        int startNum = 1;
        if (isNumberPowOfXTwo(num)) {
            return num;
        } else {
            while (true) {
                startNum = (int) startNum << 1;
                if (startNum > num) {
                    return startNum;
                }
            }
        }
    }

    public static boolean isNumberPowOfXTwo(int number)
    {
        return (Math.log(number) / Math.log(2)) % 1 == 0;
    }
}
