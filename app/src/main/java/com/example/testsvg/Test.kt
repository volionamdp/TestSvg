package com.example.testsvg


fun main() {
    val regex = Regex("\\d+")

// positive test cases, should all be "true"

// positive test cases, should all be "true"
    println("1".contains(regex))
    println("12345".matches(regex))
    println("123456789".matches(regex))

// negative test cases, should all be "false"

// negative test cases, should all be "false"
    println("".matches(regex))
    println("foo".matches(regex))
    println("aa123bb".contains(regex))
    println(checkImage("12.jpg"))
}

fun checkImage(fileName: String): Boolean {
    val regex = Regex("\\d+")
    var name = fileName
    val index = fileName.lastIndexOf(".")
    if (index > 0) {
        name = fileName.substring(0, index)
    }
    return name.matches(regex)
}
