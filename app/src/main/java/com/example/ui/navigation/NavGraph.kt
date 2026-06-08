package com.example.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ui.screens.*

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(onNavigate = { navController.navigate(it) })
        }
        composable("diploma") {
            DiplomaScreen(onNavigate = { screen, params ->
                when (screen) {
                    "books" -> {
                        val chapterName = params["chapterName"] ?: ""
                        navController.navigate("books/${Uri.encode(chapterName)}")
                    }
                    "chapter_books" -> {
                        val chapterId = params["chapterId"] ?: ""
                        val chapterName = params["chapterName"] ?: ""
                        navController.navigate("chapter/$chapterId/${Uri.encode(chapterName)}")
                    }
                    "pdf_viewer" -> {
                        val bookTitle = params["title"] ?: ""
                        val bookFilePath = params["file"] ?: ""
                        navController.navigate("pdf_viewer/${Uri.encode(bookTitle)}/${Uri.encode(bookFilePath)}")
                    }
                }
            })
        }
        composable("search") {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPdf = { book ->
                    navController.navigate("pdf_viewer/${Uri.encode(book.title)}/${Uri.encode(book.file)}")
                }
            )
        }
        composable("skills") {
            SkillsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            "books/{chapterName}",
            arguments = listOf(navArgument("chapterName") { type = NavType.StringType })
        ) { backStackEntry ->
            val chapterName = Uri.decode(backStackEntry.arguments?.getString("chapterName") ?: "")
            BooksScreen(
                chapterName = chapterName,
                onBack = { navController.popBackStack() },
                onNavigateToPdf = { book ->
                    navController.navigate("pdf_viewer/${Uri.encode(book.title)}/${Uri.encode(book.file)}")
                }
            )
        }
        composable(
            "chapter/{chapterId}/{chapterName}",
            arguments = listOf(
                navArgument("chapterId") { type = NavType.StringType },
                navArgument("chapterName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
            val chapterName = Uri.decode(backStackEntry.arguments?.getString("chapterName") ?: "")
            ChapterScreen(
                chapterName = chapterName,
                chapterId = chapterId,
                onBack = { navController.popBackStack() },
                onNavigate = { screen, params ->
                    when (screen) {
                        "device_subjects" -> {
                            val devName = params["deviceName"] ?: ""
                            navController.navigate("device/${params["chapterId"]}/${Uri.encode(devName)}")
                        }
                        "subject_content" -> {
                            val devName = params["deviceName"] ?: ""
                            val subTitle = params["subjectTitle"] ?: ""
                            val index = params["subjectIndex"] ?: "0"
                            val isGen = params["isGeneral"] ?: "false"
                            navController.navigate("subject/${params["chapterId"]}/${Uri.encode(devName)}/${Uri.encode(subTitle)}/$index/$isGen")
                        }
                    }
                }
            )
        }
        composable(
            "device/{chapterId}/{deviceName}",
            arguments = listOf(
                navArgument("chapterId") { type = NavType.StringType },
                navArgument("deviceName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
            val deviceName = Uri.decode(backStackEntry.arguments?.getString("deviceName") ?: "")
            DeviceScreen(
                chapterId = chapterId,
                deviceName = deviceName,
                onBack = { navController.popBackStack() },
                onSubjectClick = { index, title ->
                    navController.navigate("subject/$chapterId/${Uri.encode(deviceName)}/${Uri.encode(title)}/$index/false")
                }
            )
        }
        composable(
            "subject/{chapterId}/{deviceName}/{subjectTitle}/{subjectIndex}/{isGeneral}",
            arguments = listOf(
                navArgument("chapterId") { type = NavType.StringType },
                navArgument("deviceName") { type = NavType.StringType },
                navArgument("subjectTitle") { type = NavType.StringType },
                navArgument("subjectIndex") { type = NavType.IntType },
                navArgument("isGeneral") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
            val deviceName = Uri.decode(backStackEntry.arguments?.getString("deviceName") ?: "")
            val subjectTitle = Uri.decode(backStackEntry.arguments?.getString("subjectTitle") ?: "")
            val subjectIndex = backStackEntry.arguments?.getInt("subjectIndex") ?: 0
            val isGeneral = backStackEntry.arguments?.getBoolean("isGeneral") ?: false
            SubjectContentScreen(
                subjectTitle = subjectTitle,
                onBack = { navController.popBackStack() },
                onContentTypeClick = { contentType ->
                    navController.navigate("content_books/$chapterId/${Uri.encode(deviceName)}/$subjectIndex/${Uri.encode(contentType)}/${Uri.encode(subjectTitle)}/$isGeneral")
                }
            )
        }
        composable(
            "content_books/{chapterId}/{deviceName}/{subjectIndex}/{contentType}/{subjectTitle}/{isGeneral}",
            arguments = listOf(
                navArgument("chapterId") { type = NavType.StringType },
                navArgument("deviceName") { type = NavType.StringType },
                navArgument("subjectIndex") { type = NavType.IntType },
                navArgument("contentType") { type = NavType.StringType },
                navArgument("subjectTitle") { type = NavType.StringType },
                navArgument("isGeneral") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
            val deviceName = Uri.decode(backStackEntry.arguments?.getString("deviceName") ?: "")
            val subjectIndex = backStackEntry.arguments?.getInt("subjectIndex") ?: 0
            val contentType = Uri.decode(backStackEntry.arguments?.getString("contentType") ?: "")
            val subjectTitle = Uri.decode(backStackEntry.arguments?.getString("subjectTitle") ?: "")
            val isGeneral = backStackEntry.arguments?.getBoolean("isGeneral") ?: false
            ContentBooksScreen(
                chapterId = chapterId,
                deviceName = deviceName,
                subjectIndex = subjectIndex,
                contentType = contentType,
                subjectTitle = subjectTitle,
                isGeneral = isGeneral,
                onBack = { navController.popBackStack() },
                onNavigateToPdf = { book ->
                    navController.navigate("pdf_viewer/${Uri.encode(book.title)}/${Uri.encode(book.file)}")
                }
            )
        }
        composable(
            "pdf_viewer/{bookTitle}/{bookFilePath}",
            arguments = listOf(
                navArgument("bookTitle") { type = NavType.StringType },
                navArgument("bookFilePath") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val bookTitle = Uri.decode(backStackEntry.arguments?.getString("bookTitle") ?: "")
            val bookFilePath = Uri.decode(backStackEntry.arguments?.getString("bookFilePath") ?: "")
            PdfViewerScreen(
                bookTitle = bookTitle,
                bookFilePath = bookFilePath,
                onBack = { navController.popBackStack() }
            )
        }
        composable("directory") {
            DirectoryScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPdf = { book ->
                    navController.navigate("pdf_viewer/${Uri.encode(book.title)}/${Uri.encode(book.file)}")
                },
                onNavigateToChapter = { chapterId, chapterName ->
                    navController.navigate("chapter/$chapterId/${Uri.encode(chapterName)}")
                },
                onNavigateToBooks = { chapterName ->
                    navController.navigate("books/${Uri.encode(chapterName)}")
                }
            )
        }
        composable("reports") {
            ReportsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("calculators") {
            CalculatorsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("inventory") {
            InventoryDashboardScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("qr_scanner") {
            QrScannerScreen(
                onBack = { navController.popBackStack() },
                onNavigateToPdf = { book ->
                    navController.navigate("pdf_viewer/${Uri.encode(book.title)}/${Uri.encode(book.file)}")
                }
            )
        }
        composable("simulation") {
            SimulationCenterScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
