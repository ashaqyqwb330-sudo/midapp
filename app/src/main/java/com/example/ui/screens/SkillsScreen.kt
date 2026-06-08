package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

data class MedicalSkill(
    val icon: String,
    val title: String,
    val category: String,
    val description: String,
    val steps: List<String>
)

@Composable
fun SkillsScreen(onBack: () -> Unit) {
    val skills = remember {
        listOf(
            MedicalSkill(
                icon = "🩸",
                title = "بروتوكول فرز المصابين المتقدم (Triage)",
                category = "الطب العسكري الميداني",
                description = "نظام الفرز الفوري لتحديد أولويات العلاج والنقل الطبي في ساحة العمليات والاشتباكات.",
                steps = listOf(
                    "المرحلة الأولى: فحص التنفس (إذا كان متوقفًا وغير متاح، يوضع كعلامة سوداء).",
                    "المرحلة الثانية: فحص الدوران والنبض الكعبري والنزيف الخارجي الحاد.",
                    "المرحلة الثالثة: تقييم الحالة العقلية والاستجابة للأوامر البسيطة.",
                    "المرحلة الرابعة: التصنيف اللوني (أحمر: فوري، أصفر: متوسط، أخضر: طفيف، أسود: متوفي)."
                )
            ),
            MedicalSkill(
                icon = "🫁",
                title = "بروتوكول فحص وظائف الرئتين والتنفس الميداني (Pulmonary)",
                category = "مهارات بلكات أجهزة الجسم الحيوية",
                description = "التحقق الميداني والسريري من كفاءة التهوية الرئوية وتأمين المجرى الهوائي في حالات الإصابات الصدرية.",
                steps = listOf(
                    "التحقق من تزويد الأوكسجين وتصريف مجاري الهواء دون أي عائق رئوي.",
                    "تحديد عمق ومعدل التنفس الطبيعي وعلامات الجهد التنفسي الإضافي.",
                    "تقييم تمدد القفص الصدري الثنائي واستنشاق أصوات الحويصلات الهوائية.",
                    "الإنعاش التنفسي الفوري بإبرة تفريغ الضغط الهوائي الصدري عند الحاجة."
                )
            ),
            MedicalSkill(
                icon = "🫀",
                title = "الفحص السريري ونقاط الإنعاش القلبي المتقدم (Cardiovascular)",
                category = "مهارات بلكات أجهزة الجسم الحيوية",
                description = "بروتوكول التدخل السريع لعلاج الرجفان القلبي والمحافظة على التروية الدموية للأنسجة الحيوية.",
                steps = listOf(
                    "التأكد من سلامة النبض المركزي (السباتي) وغياب الحركات التنفيسية الارتجافية.",
                    "البدء الفوري بالضغطات الصدرية بمعدل 100-120 ضغطة في الدقيقة وبعمق 5-6 سم.",
                    "مزامنة الضغط مع التهوية بمعدل 30 ضغطة إلى نفسين كاملين (30:2).",
                    "استخدام جهاز إزالة الرجفان وتحليل نظم القلب الكهربي لاستعادة التروية المثالية فورا."
                )
            ),
            MedicalSkill(
                icon = "🧼",
                title = "تحليل وظائف الكليتين والميزان الهيدروليكي (Renal)",
                category = "مهارات بلكات أجهزة الجسم الحيوية",
                description = "سحب وقراءة مؤشرات غازات الدم الشرياني والشوارد لتشخيص سلامة الميزان الحمضي والكلوي.",
                steps = listOf(
                    "إجراء اختبار سلامة التروية الشريانية للأطراف المحيطية قبل سحب عينات الدم.",
                    "قياس مستويات الكرياتينين واليوريا لتحديد كفاءة الفلترة الكبيبية الكلوية.",
                    "تقييم توازن السوائل والشوارد (الصوديوم والبوتاسيوم) وعلاقتها بضغط الدم الكلي.",
                    "مقارنة الرقم الهيدروجيني للتفريق بين الاضطرابات الكلوية والاضطرابات التنفسية الأيضية."
                )
            ),
            MedicalSkill(
                icon = "🦴",
                title = "تقييم وتدبير إصابات الهيكل العضلي الحادة (Musculoskeletal)",
                category = "مهارات بلكات أجهزة الجسم الحيوية",
                description = "بروتوكول تفحص وتثبيت الكسور ومتابعة التروية الدموية الطرفية وحماية الأعصاب المجاورة في الميدان.",
                steps = listOf(
                    "تقييم النبض والتروية وتفحص الإحساس الطرفي والحركة (PMS) قبل وبعد أي تثبيت.",
                    "تثبيت المفصلين الواقعين أعلى وأسفل الكسر لضمان الثبات الكامل للطرف المصاب.",
                    "تغطية الجروح المرافقة للكسور المفتوحة بشاش معقم ودون محاولة إرجاع العظم البارز.",
                    "مراقبة حدوث متلازمة الحجرات الحيوية (Compartment Syndrome) وقياس شدة الألم المقاوم للجرعات."
                )
            ),
            MedicalSkill(
                icon = "🍕",
                title = "تقييم البطن الجراحية الحادة والبلعوم (Gastrointestinal)",
                category = "مهارات بلكات أجهزة الجسم الحيوية",
                description = "مهارة الفحص السريري للبطن للتمييز بين الآلام الوظيفية والحالات التي تطلب تداخلاً جراحيًا فوريًا في الميدان.",
                steps = listOf(
                    "تطبيق بروتوكول فحص البطن بالترتيب المعتمد للبطن السريرية (تأمل، صمت، قرع، ثم جس).",
                    "التحري الفوري عن علامة الارتداد المؤلم (Rebound Tenderness) وعلامة ميرفي لتحديد التهابات الملحقات والجراحية.",
                    "تقييم أصوات الأمعاء الطبيعية بالإنصات (الغياب الكامل للأصوات يشير إلى شلل معوي أو انسداد حاد).",
                    "فحص البلعوم والتأكد من منعكس التقيؤ لضمان سلامة حماية مجرى الهواء لتفادي الاستنشاق الرئوي."
                )
            ),
            MedicalSkill(
                icon = "🧠",
                title = "الفحص والتحري العصبي المتقدم (Nervous System)",
                category = "مهارات بلكات أجهزة الجسم الحيوية",
                description = "تقييم مستوى الوعي والوظائف الدماغية والأعصاب القحفية باستخدام مقياس غلاسكو وتفاعل حدقة العين.",
                steps = listOf(
                    "تحديد حجم وتماثل وتفاعل حدقتي العين للضوء لاستبعاد ارتفاع الضغط داخل القحف.",
                    "تطبيق مقياس غلاسكو للوعي (GCS) من أصل 15 عبر تفحص الاستجابة العينية واللفظية والحركية.",
                    "تقييم قوة العضلات المتماثلة والمنعكسات الوترية العميقة للطرفين العلوي والسفلي.",
                    "التحري الفوري عن علامات صلابة النقرة وصلابة الرقبة لاستبعاد التهاب السحايا الحاد."
                )
            )
        )
    }

    var expandedTitle by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF030810), Primary, Color(0xFF0F1F33)))
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onBack() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔙 عودة للرئيسية",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextGold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "دليل المهارات السريرية للبلكات والأجهزة",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextGold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(skills) { skill ->
                val isExpanded = expandedTitle == skill.title
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedTitle = if (isExpanded) null else skill.title
                        }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = skill.icon, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = skill.title,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextGold
                                )
                                Text(
                                    text = skill.category,
                                    fontSize = 12.sp,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = skill.description,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )

                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                Text(
                                    text = "خطوات التنفيذ الميداني المعتمدة:",
                                    fontSize = 14.sp,
                                    color = Secondary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                skill.steps.forEachIndexed { index, step ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = "${index + 1}.  ",
                                            color = Secondary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = step,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
