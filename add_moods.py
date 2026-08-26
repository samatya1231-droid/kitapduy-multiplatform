path = r'composeApp\src\commonMain\kotlin\com\semtinazizi\kitapduy\ui\screens\HomeScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    c = f.read()

search_bar_end = '''                                    Text("Kitap, yazar veya kategori ara...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 14.sp)
                                }
                            }
                        }
                    }'''

mood_block = '''
                    // 2.5 QUICK ACCESS GRID (Moods)
                    item {
                        val quickAccessMoods = com.semtinazizi.kitapduy.model.getMoodList().take(6)
                        if (quickAccessMoods.isNotEmpty()) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                quickAccessMoods.chunked(2).forEach { rowMoods ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowMoods.forEach { mood ->
                                            MoodQuickAccessCard(
                                                mood = mood,
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    // Navigator is not yet set up for MoodDetailScreen in KMP, just placeholder
                                                }
                                            )
                                        }
                                        if (rowMoods.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }'''

c = c.replace(search_bar_end, search_bar_end + "\n" + mood_block)

with open(path, 'w', encoding='utf-8') as f:
    f.write(c)
print("Added Moods to HomeScreen")
