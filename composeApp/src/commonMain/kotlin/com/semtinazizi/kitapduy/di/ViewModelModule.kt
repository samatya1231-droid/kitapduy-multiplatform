package com.semtinazizi.kitapduy.di

import com.semtinazizi.kitapduy.ui.viewmodel.AuthViewModel
import com.semtinazizi.kitapduy.ui.viewmodel.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { HomeViewModel(get(), get()) }
    viewModel { AuthViewModel(get(), get(), get()) }
    viewModel { com.semtinazizi.kitapduy.ui.viewmodel.PodcastEpisodesViewModel(get()) }
    factory { com.semtinazizi.kitapduy.ui.viewmodel.FeedScreenModel(get<com.semtinazizi.kitapduy.domain.repository.FeedRepository>(), get<com.semtinazizi.kitapduy.data.repository.StorageRepository>(), get(), getOrNull()) }
    factory { com.semtinazizi.kitapduy.ui.viewmodel.LibraryScreenModel(get(), get(), get(), get()) }
    factory { com.semtinazizi.kitapduy.ui.viewmodel.FocusScreenModel(get()) }
    factory { com.semtinazizi.kitapduy.ui.viewmodel.QuizScreenModel(get(), get()) }
    factory { com.semtinazizi.kitapduy.ui.viewmodel.AskidaViewModel(get(), get(), getOrNull()) }
    factory { com.semtinazizi.kitapduy.ui.viewmodel.SearchScreenModel(get(), get()) }
    factory { com.semtinazizi.kitapduy.ui.viewmodel.ChatScreenModel(get(), it.get()) }
    factory { com.semtinazizi.kitapduy.ui.viewmodel.ProfileScreenModel(get(), get(), get()) }
    factory { com.semtinazizi.kitapduy.ui.viewmodel.BookDetailScreenModel(get(), get(), getOrNull(), getOrNull(), getOrNull()) }
    factory { com.semtinazizi.kitapduy.ui.viewmodel.GuessAuthorScreenModel() }
    factory { com.semtinazizi.kitapduy.ui.viewmodel.PostDetailScreenModel(get()) }
    factory { com.semtinazizi.kitapduy.ui.screens.LiteraryAdvisorScreenModel() }
}

