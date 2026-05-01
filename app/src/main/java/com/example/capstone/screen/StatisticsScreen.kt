package com.example.capstone.screen
// 통계화면
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.capstone.ui.theme.CapstoneTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// ──────────────────────────────────────────────
// 1. 데이터 모델 (나중에 DB Entity로 교체 예정)
// ──────────────────────────────────────────────

data class InspectionRecord(
    val id: Int,
    val sequence: Int,          // 몇 차 검사
    val date: LocalDate,
    val time: String,           // "14:30"
    val totalCount: Int,
    val errorCount: Int,
) {
    val hasError: Boolean get() = errorCount > 0
}

// 더미 데이터 생성기 — DB 연동 시 Repository로 교체
object InspectionDummyData {
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun getRecordsForDate(date: LocalDate): List<InspectionRecord> {
        // 실제 구현 시: repository.getByDate(date)
        return when (date.format(dateFormatter)) {
            LocalDate.now().format(dateFormatter) -> listOf(
                InspectionRecord(1, 1, date, "14:30", 50, 3),
                InspectionRecord(2, 2, date, "13:15", 40, 0),
                InspectionRecord(3, 3, date, "11:45", 45, 2),
            )
            LocalDate.now().minusDays(1).format(dateFormatter) -> listOf(
                InspectionRecord(4, 1, date, "10:20", 50, 1),
                InspectionRecord(5, 2, date, "09:00", 48, 0),
            )
            else -> emptyList()
        }
    }

    /** 검사 기록이 존재하는 날짜 집합 (캘린더 강조 표시용) */
    fun getAvailableDates(): Set<LocalDate> = setOf(
        LocalDate.now(),
        LocalDate.now().minusDays(1),
        LocalDate.now().minusDays(3),
        LocalDate.now().minusDays(7),
    )
}

// ──────────────────────────────────────────────
// 2. 통계 화면 진입점 (상태 기반 내부 라우팅)
//    NavController 도입 시 이 함수를 navGraph로 분리하면 됨
// ──────────────────────────────────────────────

private sealed class StatScreen {
    object DateSelection : StatScreen()
    data class InspectionList(val date: LocalDate) : StatScreen()
}

@Composable
fun StatisticsRootScreen(
    onBack: () -> Unit,                          // 홈으로 돌아가기
    onInspectionItemClick: (InspectionRecord) -> Unit = {}, // 상세 화면 연결 예정
) {
    var currentScreen by remember { mutableStateOf<StatScreen>(StatScreen.DateSelection) }

    when (val screen = currentScreen) {
        is StatScreen.DateSelection -> {
            DateSelectionScreen(
                onBack = onBack,
                onDateConfirmed = { date ->
                    currentScreen = StatScreen.InspectionList(date)
                },
            )
        }
        is StatScreen.InspectionList -> {
            val records = remember(screen.date) {
                InspectionDummyData.getRecordsForDate(screen.date)
            }
            InspectionListScreen(
                selectedDate = screen.date,
                records = records,
                onBack = { currentScreen = StatScreen.DateSelection },
                onItemClick = onInspectionItemClick,
            )
        }
    }
}

// ──────────────────────────────────────────────
// 3. 날짜 선택 화면
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectionScreen(
    onBack: () -> Unit,
    onDateConfirmed: (LocalDate) -> Unit,
) {
    val today = remember { LocalDate.now() }
    var selectedDate by remember { mutableStateOf(today) }
    var currentYearMonth by remember { mutableStateOf(YearMonth.from(today)) }
    val availableDates = remember { InspectionDummyData.getAvailableDates() }

    val primaryBlue = Color(0xFF2563EB)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "오류 통계",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        )
                        Text(
                            text = "확인할 날짜를 선택해주세요",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 선택된 날짜 표시 카드
            SelectedDateCard(selectedDate = selectedDate, primaryBlue = primaryBlue)

            // 달력
            CalendarCard(
                yearMonth = currentYearMonth,
                selectedDate = selectedDate,
                availableDates = availableDates,
                primaryBlue = primaryBlue,
                onPreviousMonth = { currentYearMonth = currentYearMonth.minusMonths(1) },
                onNextMonth = { currentYearMonth = currentYearMonth.plusMonths(1) },
                onDateSelected = { selectedDate = it },
            )

            Spacer(modifier = Modifier.weight(1f))

            // 하단 확인 버튼
            Button(
                onClick = { onDateConfirmed(selectedDate) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
            ) {
                Text(
                    text = "이 날짜의 검사 목록 보기",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun SelectedDateCard(selectedDate: LocalDate, primaryBlue: Color) {
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy.MM.dd") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "선택한 날짜",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = selectedDate.format(formatter),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = primaryBlue,
            )
        }
    }
}

@Composable
private fun CalendarCard(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    availableDates: Set<LocalDate>,
    primaryBlue: Color,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 월 이동 헤더
            CalendarHeader(
                yearMonth = yearMonth,
                primaryBlue = primaryBlue,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 요일 헤더
            DayOfWeekHeader()
            Spacer(modifier = Modifier.height(8.dp))

            // 날짜 그리드
            CalendarGrid(
                yearMonth = yearMonth,
                selectedDate = selectedDate,
                availableDates = availableDates,
                primaryBlue = primaryBlue,
                onDateSelected = onDateSelected,
            )
        }
    }
}

@Composable
private fun CalendarHeader(
    yearMonth: YearMonth,
    primaryBlue: Color,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "이전 달")
        }
        Text(
            text = "${yearMonth.year}년 ${yearMonth.monthValue}월",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        IconButton(onClick = onNextMonth) {
            Icon(Icons.Default.ChevronRight, contentDescription = "다음 달")
        }
    }
}

@Composable
private fun DayOfWeekHeader() {
    val days = listOf("일", "월", "화", "수", "목", "금", "토")
    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEachIndexed { index, day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = when (index) {
                    0 -> Color(0xFFDC2626)   // 일 → 빨간색
                    6 -> Color(0xFF2563EB)   // 토 → 파란색
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                },
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    selectedDate: LocalDate,
    availableDates: Set<LocalDate>,
    primaryBlue: Color,
    onDateSelected: (LocalDate) -> Unit,
) {
    // 이달 1일의 요일 오프셋 계산 (일=0, 월=1, ... 토=6)
    val firstDayOfMonth = yearMonth.atDay(1)
    val startOffset = when (firstDayOfMonth.dayOfWeek) {
        DayOfWeek.SUNDAY    -> 0
        DayOfWeek.MONDAY    -> 1
        DayOfWeek.TUESDAY   -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY  -> 4
        DayOfWeek.FRIDAY    -> 5
        DayOfWeek.SATURDAY  -> 6
    }
    val daysInMonth = yearMonth.lengthOfMonth()
    val totalCells = startOffset + daysInMonth
    val rows = (totalCells + 6) / 7  // 올림 나눗셈

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    val dayNumber = cellIndex - startOffset + 1
                    val isValidDay = dayNumber in 1..daysInMonth
                    val date = if (isValidDay) yearMonth.atDay(dayNumber) else null

                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                isSelected = date == selectedDate,
                                hasRecord = date in availableDates,
                                primaryBlue = primaryBlue,
                                dayOfWeek = col,
                                onClick = { onDateSelected(date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    hasRecord: Boolean,
    primaryBlue: Color,
    dayOfWeek: Int, // 0=일, 6=토
    onClick: () -> Unit,
) {
    val textColor = when {
        isSelected  -> Color.White
        dayOfWeek == 0 -> Color(0xFFDC2626)
        dayOfWeek == 6 -> primaryBlue
        else -> MaterialTheme.colorScheme.onSurface
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(2.dp)
            .clip(CircleShape)
            .background(if (isSelected) primaryBlue else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            textAlign = TextAlign.Center,
        )
        // 검사 기록 있는 날짜 → 점 표시
        if (hasRecord && !isSelected) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(primaryBlue),
            )
        }
    }
}

// ──────────────────────────────────────────────
// 4. 검사 목록 화면
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionListScreen(
    selectedDate: LocalDate,
    records: List<InspectionRecord>,
    onBack: () -> Unit,
    onItemClick: (InspectionRecord) -> Unit,
) {
    val primaryBlue = Color(0xFF2563EB)
    val dateLabel = remember(selectedDate) {
        selectedDate.format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${dateLabel} 검사 목록",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                        )
                        Text(
                            text = "검사를 선택하여 상세 정보를 확인하세요",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (records.isEmpty()) {
            EmptyRecordsView(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Text(
                        text = "총 ${records.size}개의 검사",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                items(
                    items = records,
                    key = { it.id },
                ) { record ->
                    InspectionRecordCard(
                        record = record,
                        onClick = { onItemClick(record) },
                    )
                }
            }
        }
    }
}

@Composable
private fun InspectionRecordCard(
    record: InspectionRecord,
    onClick: () -> Unit,
) {
    val primaryBlue = Color(0xFF2563EB)
    val statusColor = if (record.hasError) Color(0xFFDC2626) else Color(0xFF16A34A)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 검사 번호 배지
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(primaryBlue.copy(alpha = 0.1f))
                    .border(1.dp, primaryBlue.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${record.sequence}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryBlue,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "검사 #${record.sequence}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (record.hasError)
                        "오류 발견 - ${record.errorCount}건"
                    else
                        "검사 정상 - 총 ${record.totalCount}개",
                    fontSize = 13.sp,
                    color = statusColor,
                    fontWeight = FontWeight.Medium,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = record.time,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor),
                )
            }
        }
    }
}

@Composable
private fun EmptyRecordsView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "📋", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "해당 날짜의 검사 기록이 없습니다",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            )
        }
    }
}

// ──────────────────────────────────────────────
// 5. Preview
// ──────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun DateSelectionScreenPreview() {
    CapstoneTheme {
        DateSelectionScreen(
            onBack = {},
            onDateConfirmed = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InspectionListScreenPreview() {
    val today = LocalDate.now()
    CapstoneTheme {
        InspectionListScreen(
            selectedDate = today,
            records = InspectionDummyData.getRecordsForDate(today),
            onBack = {},
            onItemClick = {},
        )
    }
}