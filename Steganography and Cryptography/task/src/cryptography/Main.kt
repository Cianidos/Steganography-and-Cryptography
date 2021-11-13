package cryptography

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.experimental.xor

fun BufferedImage.hideMessage(bytes: ByteArray): BufferedImage {
    var x = 0
    var y = 0

    bytes.forEach { byte ->
        val int = byte.toInt()

        (7 downTo 0).forEach {
            val bit = (int shr it) and 1
            val oldRGB = getRGB(x, y)
            val newRGB = (oldRGB shr 1 shl 1) or bit
            setRGB(x, y, newRGB)

            x++
            if (x >= width) {
                x = 0
                y++
            }
        }
    }
    return this
}

fun BufferedImage.readMessage(): ByteArray {
    val endSuffix = mutableListOf<Byte>(0, 0, 3)
    val bytes: MutableList<Byte> = mutableListOf()
    var bitHolder = 0
    var bitShift = 7

    pixelLoop@ for (y in 0 until height) {
        for (x in 0 until width) {
            val rgb = getRGB(x, y)
            val bit = rgb and 1
            bitHolder = bitHolder or (bit shl bitShift)
            bitShift--

            if (bitShift == -1) {
                val byte = bitHolder.toByte()
                bytes.add(byte)

                if (bytes.takeLast(3) == endSuffix)
                    break@pixelLoop

                bitHolder = 0
                bitShift = 7
            }
        }
    }

    return bytes.take(bytes.size - endSuffix.size + 1)
        .toByteArray()
}

tailrec fun repl() {
    println("Task (hide, show, exit):")
    when (val cmd = readLine().orEmpty()) {
        "hide" -> {
            println("Input image file:")
            val inpPath = readLine().orEmpty()
            println("Output image file:")
            val otpPath = readLine().orEmpty()
            println("Message to hide:")
            val message = readLine().orEmpty()
            println("Password:")
            val password = readLine().orEmpty().toByteArray()
            val encryptedMessageByteArray = message
                .encodeToByteArray().encrypt(password) +
                    byteArrayOf(0, 0, 3)
            File(inpPath).runCatching {
                ImageIO.read(this)
            }.onFailure {
                println("Can't read input file!")
            }.mapCatching {
                require(it.width * it.height >= encryptedMessageByteArray.size * 8)
                val new = it.hideMessage(encryptedMessageByteArray)
                ImageIO.write(new, "png", File(otpPath))
            }.onFailure {
                println(
                    "The input image is not large enough to hold this message."
                )
            }.onSuccess {
                println("Input Image: $inpPath")
                println("Output Image: $otpPath")
                println("Message saved in $otpPath image.")
            }
        }
        "show" -> {
            println("Input image file:")
            val inpPath = readLine().orEmpty()
            println("Password:")
            val password = readLine().orEmpty().encodeToByteArray()
            File(inpPath).runCatching {
                ImageIO.read(this)
            }.onFailure {
                println("Can't read input file!")
            }.onSuccess {
                println("Message:")
                println(
                    it.readMessage().decode(password)
                        .toString(Charsets.UTF_8)
                )
            }
        }
        "exit" -> {
            println("Bye!")
            return
        }
        else -> println("Wrong task: $cmd")
    }
    repl()
}

fun ByteArray.encrypt(password: ByteArray): ByteArray {
    var passIdx = 0
    return map { it xor password[passIdx++ % password.size] }
        .toByteArray()
}

fun ByteArray.decode(password: ByteArray): ByteArray {
    var passIdx = 0
    return map { it xor password[passIdx++ % password.size] }
        .toByteArray()
}

fun main() {
    repl()
}
