package com.example.mangaapp.repository

import com.example.mangaapp.models.Chapter
import com.example.mangaapp.models.Manga
import com.example.mangaapp.models.MangaStatus

object MangaRepository {

    // Dữ liệu mẫu
    private val fakeMangas = listOf(
        Manga(
            id = 1,
            name = "Naruto",
            slug = "naruto",
            author = "Masashi Kishimoto",
            description = "Câu chuyện về ninja Naruto Uzumaki với ước mơ trở thành Hokage vĩ đại nhất làng Lá.",
            coverUrl = "https://picsum.photos/seed/naruto/200/280",
            genres = listOf("Hành Động", "Phiêu Lưu"),
            totalChapters = 700,
            totalViews = 850000,
            status = MangaStatus.COMPLETED
        ),
        Manga(
            id = 2,
            name = "One Piece",
            slug = "one-piece",
            author = "Eiichiro Oda",
            description = "Hành trình của Monkey D. Luffy cùng băng hải tặc Mũ Rơm tìm kho báu One Piece.",
            coverUrl = "https://picsum.photos/seed/onepiece/200/280",
            genres = listOf("Hành Động", "Phiêu Lưu", "Hài Hước"),
            totalChapters = 1100,
            totalViews = 1200000,
            status = MangaStatus.ONGOING
        ),
        Manga(
            id = 3,
            name = "Demon Slayer",
            slug = "demon-slayer",
            author = "Koyoharu Gotouge",
            description = "Tanjiro chiến đấu với ác quỷ để cứu em gái Nezuko đã bị biến thành quỷ.",
            coverUrl = "https://picsum.photos/seed/demonslayer/200/280",
            genres = listOf("Hành Động"),
            totalChapters = 205,
            totalViews = 700000,
            status = MangaStatus.COMPLETED
        ),
        Manga(
            id = 4,
            name = "Doraemon",
            slug = "doraemon",
            author = "Fujiko F. Fujio",
            description = "Chú mèo máy đến từ tương lai với chiếc túi thần kỳ giúp đỡ cậu bé Nobita.",
            coverUrl = "https://picsum.photos/seed/doraemon/200/280",
            genres = listOf("Hài Hước"),
            totalChapters = 1344,
            totalViews = 950000,
            status = MangaStatus.COMPLETED
        ),
        Manga(
            id = 5,
            name = "Attack on Titan",
            slug = "attack-on-titan",
            author = "Hajime Isayama",
            description = "Nhân loại chiến đấu sinh tồn chống lại những người khổng lồ Titan tàn bạo.",
            coverUrl = "https://picsum.photos/seed/aot/200/280",
            genres = listOf("Hành Động", "Kinh Dị"),
            totalChapters = 139,
            totalViews = 600000,
            status = MangaStatus.COMPLETED
        ),
        Manga(
            id = 6,
            name = "Dragon Ball",
            slug = "dragon-ball",
            author = "Akira Toriyama",
            description = "Hành trình của Son Goku từ nhỏ đến khi trở thành chiến binh mạnh nhất vũ trụ.",
            coverUrl = "https://picsum.photos/seed/dragonball/200/280",
            genres = listOf("Hành Động", "Phiêu Lưu"),
            totalChapters = 519,
            totalViews = 800000,
            status = MangaStatus.COMPLETED
        )
    )

    private val fakeChapters = mapOf(
        1 to listOf(
            Chapter(1, 1, 1, "Uzumaki Naruto",
                listOf("https://picsum.photos/seed/n1p1/800/1200",
                    "https://picsum.photos/seed/n1p2/800/1200")),
            Chapter(2, 1, 2, "Kẻ thù đầu tiên",
                listOf("https://picsum.photos/seed/n2p1/800/1200",
                    "https://picsum.photos/seed/n2p2/800/1200")),
            Chapter(3, 1, 3, "Kỹ thuật ninja",
                listOf("https://picsum.photos/seed/n3p1/800/1200"))
        ),
        2 to listOf(
            Chapter(4, 2, 1, "Tôi là Luffy",
                listOf("https://picsum.photos/seed/op1p1/800/1200",
                    "https://picsum.photos/seed/op1p2/800/1200")),
            Chapter(5, 2, 2, "Zoro - Thợ săn tiền thưởng",
                listOf("https://picsum.photos/seed/op2p1/800/1200"))
        ),
        3 to listOf(
            Chapter(6, 3, 1, "Tanjiro và Nezuko",
                listOf("https://picsum.photos/seed/ds1p1/800/1200",
                    "https://picsum.photos/seed/ds1p2/800/1200"))
        ),
        4 to listOf(
            Chapter(7, 4, 1, "Doraemon đến!",
                listOf("https://picsum.photos/seed/dr1p1/800/1200"))
        )
    )

    val genres = listOf("Tất cả", "Hành Động", "Tình Cảm", "Hài Hước", "Phiêu Lưu", "Kinh Dị")

    // ===================== PUBLIC API =====================
    // Các hàm bên dưới là interface dùng chung cho toàn app.
    // Khi có backend thật, chỉ sửa phần thân hàm, không sửa chữ ký hàm.

    /** Lấy toàn bộ danh sách truyện */
    fun getAllManga(): List<Manga> = fakeMangas

    /** Lấy truyện nổi bật (top views) cho banner/featured */
    fun getFeaturedManga(): List<Manga> = fakeMangas.sortedByDescending { it.totalViews }.take(4)

    /** Lấy truyện mới cập nhật */
    fun getLatestManga(): List<Manga> = fakeMangas.take(6)

    /** Lấy bảng xếp hạng top 10 */
    fun getRankingManga(): List<Manga> = fakeMangas.sortedByDescending { it.totalViews }

    /** Tìm kiếm truyện theo tên hoặc tác giả */
    fun searchManga(query: String): List<Manga> {
        if (query.isBlank()) return fakeMangas
        return fakeMangas.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.author.contains(query, ignoreCase = true)
        }
    }

    /** Lọc theo thể loại */
    fun getMangaByGenre(genre: String): List<Manga> {
        if (genre == "Tất cả" || genre.isBlank()) return fakeMangas
        return fakeMangas.filter { it.genres.contains(genre) }
    }

    /** Lấy chi tiết một truyện theo id */
    fun getMangaById(id: Int): Manga? = fakeMangas.find { it.id == id }

    /** Lấy danh sách chương của một truyện */
    fun getChaptersByMangaId(mangaId: Int): List<Chapter> =
        fakeChapters[mangaId] ?: emptyList()

    /** Lấy nội dung một chương */
    fun getChapter(mangaId: Int, chapterNumber: Int): Chapter? =
        fakeChapters[mangaId]?.find { it.chapterNumber == chapterNumber }
}
