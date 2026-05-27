package com.example.mangaapp.repository

import com.example.mangaapp.models.Chapter
import com.example.mangaapp.models.Manga
import com.example.mangaapp.models.MangaCategory
import com.example.mangaapp.models.MangaStatus
import com.google.firebase.firestore.FirebaseFirestore

object MangaRepository {

    private val fakeMangas = listOf(
        Manga(id = 1, name = "Naruto", slug = "naruto", author = "Masashi Kishimoto",
            description = "Câu chuyện về ninja Naruto Uzumaki với ước mơ trở thành Hokage vĩ đại nhất làng Lá.",
            coverUrl = "https://m.media-amazon.com/images/M/MV5BZTNjOWI0ZTAtOGY1OS00ZGU0LWEyOWYtMjhkYjdlYmVjMDk2XkEyXkFqcGc@._V1_.jpg",
            genres = listOf("Hành Động", "Phiêu Lưu"), totalChapters = 700, totalViews = 850000,
            status = MangaStatus.COMPLETED, category = MangaCategory.TRUYEN_TRANH,
            firestoreId = "naruto"),

        Manga(id = 2, name = "One Piece", slug = "one-piece", author = "Eiichiro Oda",
            description = "Hành trình của Monkey D. Luffy cùng băng hải tặc Mũ Rơm tìm kho báu One Piece.",
            coverUrl = "https://nipponclass.jp/wp-content/uploads/2024/07/MV5BM2YwYTkwNjItNGQzNy00MWE1LWE1M2ItOTMzOGI1OWQyYjA0XkEyXkFqcGdeQXVyMTUzMTg2ODkz._V1_FMjpg_UX1000_-723x1024.jpg",
            genres = listOf("Hành Động", "Phiêu Lưu", "Hài Hước"), totalChapters = 1100, totalViews = 1200000,
            status = MangaStatus.ONGOING, category = MangaCategory.TRUYEN_TRANH,
            firestoreId = "one-piece"),

        Manga(id = 3, name = "Demon Slayer", slug = "demon-slayer", author = "Koyoharu Gotouge",
            description = "Tanjiro chiến đấu với ác quỷ để cứu em gái Nezuko đã bị biến thành quỷ.",
            coverUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcT02RDGhWInPbkReQzO7qcdT-l6V0fwKwnmXQ&s",
            genres = listOf("Hành Động"), totalChapters = 205, totalViews = 700000,
            status = MangaStatus.COMPLETED, category = MangaCategory.TRUYEN_TRANH,
            firestoreId = "demon-slayer"),

        Manga(id = 4, name = "Doraemon", slug = "doraemon", author = "Fujiko F. Fujio",
            description = "Chú mèo máy đến từ tương lai với chiếc túi thần kỳ giúp đỡ cậu bé Nobita.",
            coverUrl = "https://m.media-amazon.com/images/M/MV5BNTRjMDA5ZTQtNWVkMy00OTAwLWI2NmMtYjQxYWM4MTIxYWFhXkEyXkFqcGc@._V1_.jpg",
            genres = listOf("Hài Hước"), totalChapters = 1344, totalViews = 950000,
            status = MangaStatus.COMPLETED, category = MangaCategory.TRUYEN_TRANH,
            firestoreId = "doraemon"),

        Manga(id = 5, name = "Attack on Titan", slug = "attack-on-titan", author = "Hajime Isayama",
            description = "Nhân loại chiến đấu sinh tồn chống lại những người khổng lồ Titan tàn bạo.",
            coverUrl = "https://static.elle.vn/img/3k5af-aNCsR5kn13ROxp3kUv1IfN3YoglkPVXHarfjU/rs:fit:0:0/min-height:300/plain/http://www.elle.vn/app/uploads/2025/01/13/632092/poster-Dai-Chien-Nguoi-Khong-Lo-Lan-Tan-Cong-Cuoi-Cung.jpg@webp",
            genres = listOf("Hành Động", "Kinh Dị"), totalChapters = 139, totalViews = 600000,
            status = MangaStatus.COMPLETED, category = MangaCategory.TRUYEN_TRANH,
            firestoreId = "attack-on-titan"),

        Manga(id = 6, name = "Dragon Ball", slug = "dragon-ball", author = "Akira Toriyama",
            description = "Hành trình của Son Goku từ nhỏ đến khi trở thành chiến binh mạnh nhất vũ trụ.",
            coverUrl = "https://m.media-amazon.com/images/M/MGBQ0ZWE4NDYtYWY0Mi00MjE0LWI1MzctZDA1NGExYzE3N2FiXkEyXkFqcGc@._V1_FMjpg_UX1000_.jpg",
            genres = listOf("Hành Động", "Phiêu Lưu"), totalChapters = 519, totalViews = 800000,
            status = MangaStatus.COMPLETED, category = MangaCategory.TRUYEN_TRANH,
            firestoreId = "dragon-ball"),

        Manga(id = 7, name = "Đắc Nhân Tâm", slug = "dac-nhan-tam", author = "Dale Carnegie",
            description = "Cuốn sách kinh điển về nghệ thuật giao tiếp và xây dựng mối quan hệ.",
            coverUrl = "https://bizweb.dktcdn.net/thumb/grande/100/567/082/products/dacnhantam-biacung-108k-01.jpg?v=1747113724337",
            genres = listOf("Tình Cảm"), totalChapters = 30, totalViews = 500000,
            status = MangaStatus.COMPLETED, category = MangaCategory.TIEU_THUYET,
            firestoreId = "dac-nhan-tam"),

        Manga(id = 8, name = "Nhà Giả Kim", slug = "nha-gia-kim", author = "Paulo Coelho",
            description = "Câu chuyện về chàng chăn cừu Santiago theo đuổi giấc mơ tìm kho báu.",
            coverUrl = "https://upload.wikimedia.org/wikipedia/commons/c/c4/TheAlchemist.jpg",
            genres = listOf("Phiêu Lưu", "Tình Cảm"), totalChapters = 25, totalViews = 420000,
            status = MangaStatus.COMPLETED, category = MangaCategory.TIEU_THUYET,
            firestoreId = "nha-gia-kim")
    )

    private val fakeChapters = mapOf(
        1 to listOf(
            Chapter(1, 1, 1, "Uzumaki Naruto", listOf("https://picsum.photos/seed/n1p1/800/1200", "https://picsum.photos/seed/n1p2/800/1200")),
            Chapter(2, 1, 2, "Kẻ thù đầu tiên", listOf("https://picsum.photos/seed/n2p1/800/1200", "https://picsum.photos/seed/n2p2/800/1200")),
            Chapter(3, 1, 3, "Kỹ thuật ninja", listOf("https://picsum.photos/seed/n3p1/800/1200"))
        ),
        2 to listOf(
            Chapter(4, 2, 1, "Tôi là Luffy", listOf("https://picsum.photos/seed/op1p1/800/1200", "https://picsum.photos/seed/op1p2/800/1200")),
            Chapter(5, 2, 2, "Zoro - Thợ săn tiền thưởng", listOf("https://picsum.photos/seed/op2p1/800/1200"))
        ),
        3 to listOf(Chapter(6, 3, 1, "Tanjiro và Nezuko", listOf("https://picsum.photos/seed/ds1p1/800/1200", "https://picsum.photos/seed/ds1p2/800/1200"))),
        4 to listOf(Chapter(7, 4, 1, "Doraemon đến!", listOf("https://picsum.photos/seed/dr1p1/800/1200"))),
        7 to listOf(
            Chapter(8, 7, 1, "Phần 1: Kỹ thuật cơ bản", content = "Santiago là một chàng chăn cừu người Tây Ban Nha..."),
            Chapter(9, 7, 2, "Phần 2: Nghệ thuật lắng nghe", content = "Một trong những bí quyết quan trọng nhất..."),
            Chapter(10, 7, 3, "Phần 3: Tạo ấn tượng đầu tiên", content = "Ấn tượng đầu tiên được tạo ra trong vài giây...")
        ),
        8 to listOf(
            Chapter(11, 8, 1, "Chương 1: Giấc mơ tái hiện", content = "Santiago nằm mơ thấy một đứa trẻ dẫn anh đến Kim Tự Tháp..."),
            Chapter(12, 8, 2, "Chương 2: Nhà tiên tri", content = "Ở thị trấn Tarifa, Santiago gặp một bà lão..."),
            Chapter(13, 8, 3, "Chương 3: Hành trình bắt đầu", content = "Santiago quyết định bán đàn cừu để lấy tiền lên đường...")
        )
    )

    val genres = listOf("Tất cả", "Hành Động", "Tình Cảm", "Hài Hước", "Phiêu Lưu", "Kinh Dị")

    fun getAllManga(): List<Manga> = fakeMangas
    fun getMangaByCategory(category: MangaCategory): List<Manga> = fakeMangas.filter { it.category == category }
    fun getFeaturedManga(): List<Manga> = fakeMangas.sortedByDescending { it.totalViews }.take(4)
    fun getLatestManga(): List<Manga> = fakeMangas.take(6)
    fun getRankingManga(): List<Manga> = fakeMangas.sortedByDescending { it.totalViews }
    fun searchManga(query: String): List<Manga> {
        if (query.isBlank()) return fakeMangas
        return fakeMangas.filter { it.name.contains(query, ignoreCase = true) || it.author.contains(query, ignoreCase = true) }
    }
    fun getMangaByGenre(genre: String): List<Manga> {
        if (genre == "Tất cả" || genre.isBlank()) return fakeMangas
        return fakeMangas.filter { it.genres.contains(genre) }
    }
    fun getMangaById(id: Int): Manga? = fakeMangas.find { it.id == id }
    fun getChaptersByMangaId(mangaId: Int): List<Chapter> = fakeChapters[mangaId] ?: emptyList()
    fun getChapter(mangaId: Int, chapterNumber: Int): Chapter? = fakeChapters[mangaId]?.find { it.chapterNumber == chapterNumber }

    fun getChapterPagesFromFirestore(
        storyId: String,
        chapterId: String,
        onSuccess: (List<String>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val db = FirebaseFirestore.getInstance()
        db.collection("stories")
            .document(storyId)
            .collection("pages")
            .orderBy("pageNumber")
            .get()
            .addOnSuccessListener { result ->
                val pages = result.documents.mapNotNull { it.getString("url") }
                onSuccess(pages)
            }
            .addOnFailureListener { exception ->
                onError(exception)
            }
    }
}