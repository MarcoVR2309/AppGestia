package com.example.maquetagestia.controller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import com.example.maquetagestia.controller.cEquipo.MiembroEquipoItem;
import com.example.maquetagestia.model.EnlaceTarea;
import com.example.maquetagestia.model.Fase;
import com.example.maquetagestia.model.Proyecto;
import com.example.maquetagestia.model.Tarea;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GeneradorPdfProyecto {

    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 36;
    private static final int CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2); // 523 pt
    private static final float TOP_Y = 36f;
    private static final float BOTTOM_LIMIT = 790f;

    // Paleta profesional neutra / azul legible en pantalla e impresión
    private static final int COLOR_PRIMARY = Color.parseColor("#1A4D8F");
    private static final int COLOR_PRIMARY_SOFT = Color.parseColor("#EBF2FC");
    private static final int COLOR_ACCENT = Color.parseColor("#2563EB");
    private static final int COLOR_DARK_TEXT = Color.parseColor("#1E293B");
    private static final int COLOR_MUTED_TEXT = Color.parseColor("#64748B");
    private static final int COLOR_BORDER = Color.parseColor("#CBD5E1");
    private static final int COLOR_ZEBRA = Color.parseColor("#F8FAFC");
    private static final int COLOR_TRACK = Color.parseColor("#E2E8F0");
    private static final int COLOR_SUCCESS = Color.parseColor("#15803D");
    private static final int COLOR_SUCCESS_BG = Color.parseColor("#DCFCE7");
    private static final int COLOR_WARNING = Color.parseColor("#B45309");
    private static final int COLOR_WARNING_BG = Color.parseColor("#FEF3C7");
    private static final int COLOR_ERROR = Color.parseColor("#B91C1C");
    private static final int COLOR_ERROR_BG = Color.parseColor("#FEE2E2");
    private static final int COLOR_WHITE = Color.WHITE;

    private GeneradorPdfProyecto() {
    }

    public static File generarReporteProyecto(
            Context context,
            Proyecto proyecto,
            List<Fase> fasesInput,
            List<Tarea> tareasInput,
            List<MiembroEquipoItem> equipoInput,
            List<EnlaceTarea> enlacesInput,
            String usuarioEmisor
    ) throws IOException {
        if (context == null || proyecto == null) {
            throw new IllegalArgumentException("Contexto o proyecto inválido");
        }

        List<Fase> fases = fasesInput != null ? new ArrayList<>(fasesInput) : new ArrayList<>();
        Collections.sort(fases, Comparator.comparingInt(Fase::getOrden));

        List<Tarea> tareas = tareasInput != null ? new ArrayList<>(tareasInput) : new ArrayList<>();
        List<MiembroEquipoItem> equipo = equipoInput != null ? new ArrayList<>(equipoInput) : new ArrayList<>();
        List<EnlaceTarea> enlaces = enlacesInput != null ? new ArrayList<>(enlacesInput) : new ArrayList<>();

        String nombreProy = !TextUtils.isEmpty(proyecto.getNombre()) ? proyecto.getNombre().trim() : "Proyecto";
        String safeNombre = Normalizer.normalize(nombreProy, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9_-]+", "_")
                .replaceAll("^_+|_+$", "");
        if (safeNombre.isEmpty()) safeNombre = "Proyecto";
        if (safeNombre.length() > 40) safeNombre = safeNombre.substring(0, 40);

        String fechaArchivo = new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date());
        File outFile = new File(context.getCacheDir(), "Reporte_" + safeNombre + "_" + fechaArchivo + ".pdf");

        PdfDocument document = new PdfDocument();
        FileOutputStream fos = null;
        try {
            MotorDibujoPdf motor = new MotorDibujoPdf(document);
            motor.dibujarInformeCompleto(proyecto, fases, tareas, equipo, enlaces, usuarioEmisor);
            motor.finalizar();

            fos = new FileOutputStream(outFile);
            document.writeTo(fos);
            return outFile;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
            document.close();
        }
    }

    private static class TareaConFecha {
        final Tarea tarea;
        final String fechaIso;
        final long diasRestantes;

        TareaConFecha(Tarea tarea, String fechaIso, long diasRestantes) {
            this.tarea = tarea;
            this.fechaIso = fechaIso;
            this.diasRestantes = diasRestantes;
        }
    }

    private static class CargaMiembro {
        int asignadas = 0;
        int completadas = 0;
        int pendientes = 0;
    }

    private static class MotorDibujoPdf {
        private final PdfDocument doc;
        private final String fechaHoraEmision;
        private final Calendar hoyCal;

        private PdfDocument.Page currentPage;
        private Canvas canvas;
        private int pageNum = 0;
        private float y = TOP_Y;

        private final TextPaint tpBannerBrand = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint tpBannerTitle = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint tpBannerMeta = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint tpSection = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint tpSubtitle = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint tpBody = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint tpBodyBold = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint tpSmall = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint tpSmallBold = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint tpKpiNum = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint tpKpiLabel = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint tpFooter = new TextPaint(Paint.ANTI_ALIAS_FLAG);

        private final Paint pFill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint pArc = new Paint(Paint.ANTI_ALIAS_FLAG);

        MotorDibujoPdf(PdfDocument doc) {
            this.doc = doc;
            this.fechaHoraEmision = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(new Date());
            this.hoyCal = Calendar.getInstance();
            limpiarHoras(this.hoyCal);

            tpBannerBrand.setColor(Color.parseColor("#BFDBFE"));
            tpBannerBrand.setTextSize(9f);
            tpBannerBrand.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            tpBannerTitle.setColor(COLOR_WHITE);
            tpBannerTitle.setTextSize(17f);
            tpBannerTitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            tpBannerMeta.setColor(Color.parseColor("#E2E8F0"));
            tpBannerMeta.setTextSize(9f);

            tpSection.setColor(COLOR_PRIMARY);
            tpSection.setTextSize(11.5f);
            tpSection.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            tpSubtitle.setColor(COLOR_DARK_TEXT);
            tpSubtitle.setTextSize(10f);
            tpSubtitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            tpBody.setColor(COLOR_DARK_TEXT);
            tpBody.setTextSize(9.5f);

            tpBodyBold.setColor(COLOR_DARK_TEXT);
            tpBodyBold.setTextSize(9.5f);
            tpBodyBold.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            tpSmall.setColor(COLOR_MUTED_TEXT);
            tpSmall.setTextSize(8.2f);

            tpSmallBold.setColor(COLOR_DARK_TEXT);
            tpSmallBold.setTextSize(8.2f);
            tpSmallBold.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            tpKpiNum.setTextSize(16f);
            tpKpiNum.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            tpKpiLabel.setColor(COLOR_MUTED_TEXT);
            tpKpiLabel.setTextSize(8.2f);
            tpKpiLabel.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

            tpFooter.setColor(COLOR_MUTED_TEXT);
            tpFooter.setTextSize(8f);

            pStroke.setStyle(Paint.Style.STROKE);
            pStroke.setStrokeWidth(0.8f);
            pStroke.setColor(COLOR_BORDER);

            pArc.setStyle(Paint.Style.STROKE);
            pArc.setStrokeCap(Paint.Cap.ROUND);
        }

        void dibujarInformeCompleto(
                Proyecto proyecto,
                List<Fase> fases,
                List<Tarea> tareas,
                List<MiembroEquipoItem> equipo,
                List<EnlaceTarea> enlaces,
                String usuarioEmisor
        ) {
            nuevaPagina();

            // Precalcular métricas reales
            int totalTareas = tareas.size();
            int completadas = 0;
            int pendientes = 0;
            int vencidas = 0;

            List<TareaConFecha> tareasCriticas = new ArrayList<>();
            List<TareaConFecha> tareasPendientesConFecha = new ArrayList<>();
            Map<String, List<Tarea>> pendientesPorFecha = new HashMap<>();

            for (Tarea t : tareas) {
                boolean finalizada = "Finalizada".equalsIgnoreCase(t.getEstado());
                if (finalizada) {
                    completadas++;
                } else {
                    pendientes++;
                }

                String fIso = normalizarFechaIso(t.getFechaLimite());
                if (!finalizada && fIso != null) {
                    Long diasObj = calcularDiasRestantes(fIso);
                    if (diasObj != null) {
                        long dias = diasObj;
                        if (dias < 0) {
                            vencidas++;
                        }
                        TareaConFecha tcf = new TareaConFecha(t, fIso, dias);
                        tareasPendientesConFecha.add(tcf);
                        if (dias <= 3) {
                            tareasCriticas.add(tcf);
                        }
                    }
                    List<Tarea> grupo = pendientesPorFecha.get(fIso);
                    if (grupo == null) {
                        grupo = new ArrayList<>();
                        pendientesPorFecha.put(fIso, grupo);
                    }
                    grupo.add(t);
                }
            }

            Collections.sort(tareasPendientesConFecha, Comparator.comparing(a -> a.fechaIso));
            Collections.sort(tareasCriticas, Comparator.comparingLong(a -> a.diasRestantes));

            int fasesCompletadas = 0;
            for (Fase f : fases) {
                if ("Completada".equalsIgnoreCase(f.getEstado())) {
                    fasesCompletadas++;
                }
            }

            int pctGlobal;
            if (totalTareas > 0) {
                pctGlobal = (completadas * 100) / totalTareas;
            } else if ("Finalizado".equalsIgnoreCase(proyecto.getEstado())) {
                pctGlobal = 100;
            } else if (!fases.isEmpty()) {
                pctGlobal = (fasesCompletadas * 100) / fases.size();
            } else {
                pctGlobal = 0;
            }

            // 1. Encabezado Ejecutivo
            dibujarEncabezadoEjecutivo(proyecto, usuarioEmisor);

            // 2. Resumen de Desarrollo, KPIs y Atención Inmediata
            dibujarResumenYKpis(pctGlobal, totalTareas, completadas, pendientes, vencidas, tareasCriticas);

            // 3. Calendario Mensual de Tareas Faltantes + Cronograma
            dibujarCalendarioYCronograma(pendientesPorFecha, tareasPendientesConFecha);

            // 4. Avance por Fases
            dibujarAvancePorFases(fases, tareas);

            // 5. Equipo y Carga de Trabajo
            dibujarTablaEquipo(equipo, tareas);

            // 6. Detalle de Tareas por Fase
            dibujarDetalleTareasPorFase(fases, tareas);

            // 7. Anexo de Entregables (Enlaces)
            dibujarAnexoEnlaces(enlaces, tareas);
        }

        private void dibujarEncabezadoEjecutivo(Proyecto proyecto, String usuarioEmisor) {
            String nombre = !TextUtils.isEmpty(proyecto.getNombre()) ? proyecto.getNombre().trim() : "Proyecto sin nombre";
            String estado = !TextUtils.isEmpty(proyecto.getEstado()) ? proyecto.getEstado().trim() : "Activo";
            String tipo = !TextUtils.isEmpty(proyecto.getTipo()) ? proyecto.getTipo().trim() : "General";
            String fInicio = !TextUtils.isEmpty(proyecto.getFechaInicio()) ? proyecto.getFechaInicio().trim() : "Sin inicio";
            String fFin = !TextUtils.isEmpty(proyecto.getFechaFinEstimada()) ? proyecto.getFechaFinEstimada().trim() : "Sin fin";
            String emisor = !TextUtils.isEmpty(usuarioEmisor) ? usuarioEmisor.trim() : "Usuario";
            String desc = !TextUtils.isEmpty(proyecto.getDescripcion()) ? proyecto.getDescripcion().trim() : "Sin descripción registrada.";

            StaticLayout slNombre = crearLayout(nombre, tpBannerTitle, CONTENT_WIDTH - 105);
            String metaLinea = "Tipo: " + tipo + "   •   Periodo: " + fInicio + " a " + fFin;
            StaticLayout slMeta1 = crearLayout(metaLinea, tpBannerMeta, CONTENT_WIDTH);
            String metaEmision = "Generado por: " + emisor + "   •   Emisión: " + fechaHoraEmision;
            StaticLayout slMeta2 = crearLayout(metaEmision, tpBannerMeta, CONTENT_WIDTH);

            float bannerHeight = 20f + 16f + slNombre.getHeight() + 8f + slMeta1.getHeight() + 3f + slMeta2.getHeight() + 14f;

            // Banda superior en y = 0..bannerHeight
            pFill.setColor(COLOR_PRIMARY);
            canvas.drawRect(0, 0, PAGE_WIDTH, bannerHeight, pFill);

            float cursor = 18f;
            canvas.drawText("REPORTE DE PROYECTO — GESTIA", MARGIN, cursor + 9f, tpBannerBrand);

            // Badge de estado arriba a la derecha dentro de la banda
            float badgeW = Math.max(76f, tpSmallBold.measureText(estado) + 18f);
            float badgeLeft = MARGIN + CONTENT_WIDTH - badgeW;
            RectF badgeRect = new RectF(badgeLeft, cursor - 2f, MARGIN + CONTENT_WIDTH, cursor + 16f);
            pFill.setColor(COLOR_WHITE);
            canvas.drawRoundRect(badgeRect, 8f, 8f, pFill);
            TextPaint tpBadge = new TextPaint(tpSmallBold);
            tpBadge.setColor(COLOR_PRIMARY);
            float tw = tpBadge.measureText(estado);
            canvas.drawText(estado, badgeRect.centerX() - (tw / 2f), badgeRect.top + 12f, tpBadge);

            cursor += 18f;
            dibujarLayout(slNombre, MARGIN, cursor);
            cursor += slNombre.getHeight() + 8f;

            dibujarLayout(slMeta1, MARGIN, cursor);
            cursor += slMeta1.getHeight() + 3f;

            dibujarLayout(slMeta2, MARGIN, cursor);

            y = bannerHeight + 14f;

            // Caja de descripción del proyecto
            StaticLayout slDesc = crearLayout(desc, tpBody, CONTENT_WIDTH - 24);
            float boxH = 18f + slDesc.getHeight() + 10f;
            asegurarEspacio(boxH + 10f, null);

            RectF descRect = new RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + boxH);
            pFill.setColor(COLOR_ZEBRA);
            canvas.drawRoundRect(descRect, 8f, 8f, pFill);
            pStroke.setColor(COLOR_BORDER);
            canvas.drawRoundRect(descRect, 8f, 8f, pStroke);

            pFill.setColor(COLOR_PRIMARY);
            canvas.drawRoundRect(new RectF(MARGIN, y, MARGIN + 4f, y + boxH), 2f, 2f, pFill);

            canvas.drawText("DESCRIPCIÓN DEL PROYECTO", MARGIN + 12f, y + 14f, tpSmallBold);
            dibujarLayout(slDesc, MARGIN + 12f, y + 20f);
            y += boxH + 16f;
        }

        private void dibujarResumenYKpis(
                int pctGlobal,
                int totalTareas,
                int completadas,
                int pendientes,
                int vencidas,
                List<TareaConFecha> tareasCriticas
        ) {
            dibujarTituloSeccion("1. RESUMEN DE DESARROLLO Y KPIs");

            float blockH = 114f;
            asegurarEspacio(blockH + 34f, null);

            // Panel izquierdo: Donut nativo
            float leftW = 158f;
            RectF donutCard = new RectF(MARGIN, y, MARGIN + leftW, y + blockH);
            pFill.setColor(COLOR_WHITE);
            canvas.drawRoundRect(donutCard, 8f, 8f, pFill);
            pStroke.setColor(COLOR_BORDER);
            canvas.drawRoundRect(donutCard, 8f, 8f, pStroke);

            float cx = MARGIN + (leftW / 2f);
            float cy = y + 52f;
            float radius = 34f;
            RectF arcRect = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);

            pArc.setStrokeWidth(10f);
            pArc.setColor(COLOR_TRACK);
            canvas.drawArc(arcRect, 0f, 360f, false, pArc);

            if (pctGlobal > 0) {
                pArc.setColor(pctGlobal == 100 ? COLOR_SUCCESS : COLOR_PRIMARY);
                float sweep = (360f * pctGlobal) / 100f;
                canvas.drawArc(arcRect, -90f, sweep, false, pArc);
            }

            TextPaint tpDonutVal = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            tpDonutVal.setColor(COLOR_DARK_TEXT);
            tpDonutVal.setTextSize(17f);
            tpDonutVal.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            String pctTxt = pctGlobal + "%";
            canvas.drawText(pctTxt, cx - (tpDonutVal.measureText(pctTxt) / 2f), cy + 5f, tpDonutVal);

            String subDonut = "Avance global";
            canvas.drawText(subDonut, cx - (tpSmall.measureText(subDonut) / 2f), y + blockH - 12f, tpSmall);

            // Panel derecho: Grilla 2x2 de KPIs
            float rightX = MARGIN + leftW + 10f;
            float rightW = CONTENT_WIDTH - leftW - 10f; // 355 pt
            float cardW = (rightW - 8f) / 2f;
            float cardH = (blockH - 8f) / 2f;

            dibujarKpiCard(rightX, y, cardW, cardH, "Total tareas", String.valueOf(totalTareas), COLOR_PRIMARY_SOFT, COLOR_PRIMARY);
            dibujarKpiCard(rightX + cardW + 8f, y, cardW, cardH, "Completadas", String.valueOf(completadas), COLOR_SUCCESS_BG, COLOR_SUCCESS);
            dibujarKpiCard(rightX, y + cardH + 8f, cardW, cardH, "Pendientes / En progreso", String.valueOf(pendientes), COLOR_WARNING_BG, COLOR_WARNING);
            dibujarKpiCard(rightX + cardW + 8f, y + cardH + 8f, cardW, cardH, "Vencidas", String.valueOf(vencidas), COLOR_ERROR_BG, COLOR_ERROR);

            y += blockH + 10f;

            // Barra de progreso horizontal con esquinas redondeadas
            float barH = 10f;
            RectF trackRect = new RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + barH);
            pFill.setColor(COLOR_TRACK);
            canvas.drawRoundRect(trackRect, 5f, 5f, pFill);

            if (pctGlobal > 0) {
                float fillW = Math.max(barH, (CONTENT_WIDTH * pctGlobal) / 100f);
                RectF fillRect = new RectF(MARGIN, y, MARGIN + Math.min(CONTENT_WIDTH, fillW), y + barH);
                pFill.setColor(pctGlobal == 100 ? COLOR_SUCCESS : COLOR_PRIMARY);
                canvas.drawRoundRect(fillRect, 5f, 5f, pFill);
            }
            y += barH + 14f;

            // Caja de atención inmediata (condicional)
            if (!tareasCriticas.isEmpty()) {
                asegurarEspacio(46f, null);
                float headerAlertH = 22f;
                pFill.setColor(COLOR_ERROR_BG);
                RectF headRect = new RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + headerAlertH);
                canvas.drawRoundRect(headRect, 6f, 6f, pFill);

                TextPaint tpAlertHdr = new TextPaint(tpSmallBold);
                tpAlertHdr.setColor(COLOR_ERROR);
                canvas.drawText("ATENCIÓN INMEDIATA — Tareas vencidas o por vencer en ≤ 3 días (" + tareasCriticas.size() + ")",
                        MARGIN + 10f, y + 14f, tpAlertHdr);
                y += headerAlertH + 4f;

                for (TareaConFecha item : tareasCriticas) {
                    String tituloT = !TextUtils.isEmpty(item.tarea.getTitulo()) ? item.tarea.getTitulo().trim() : "Tarea";
                    String resp = !TextUtils.isEmpty(item.tarea.getResponsableNombre()) ? item.tarea.getResponsableNombre().trim() : "Sin asignar";
                    String estadoPlazo;
                    if (item.diasRestantes < 0) {
                        long atraso = Math.abs(item.diasRestantes);
                        estadoPlazo = "Vencida hace " + atraso + (atraso == 1 ? " día" : " días");
                    } else if (item.diasRestantes == 0) {
                        estadoPlazo = "Vence HOY (" + item.fechaIso + ")";
                    } else if (item.diasRestantes == 1) {
                        estadoPlazo = "Vence mañana (" + item.fechaIso + ")";
                    } else {
                        estadoPlazo = "Vence en " + item.diasRestantes + " días (" + item.fechaIso + ")";
                    }

                    String linea = "• " + tituloT + " — " + estadoPlazo + "  |  Resp: " + resp;
                    StaticLayout slCrit = crearLayout(linea, tpSmall, CONTENT_WIDTH - 20);
                    float itemH = slCrit.getHeight() + 6f;
                    asegurarEspacio(itemH, null);

                    RectF rowBg = new RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + itemH);
                    pFill.setColor(Color.parseColor("#FFF5F5"));
                    canvas.drawRect(rowBg, pFill);
                    pFill.setColor(item.diasRestantes <= 0 ? COLOR_ERROR : COLOR_WARNING);
                    canvas.drawRect(MARGIN, y, MARGIN + 3f, y + itemH, pFill);

                    dibujarLayout(slCrit, MARGIN + 10f, y + 3f);
                    y += itemH + 2f;
                }
                y += 10f;
            }
        }

        private void dibujarKpiCard(float x, float top, float w, float h, String label, String valor, int bgColor, int accentColor) {
            RectF rect = new RectF(x, top, x + w, top + h);
            pFill.setColor(bgColor);
            canvas.drawRoundRect(rect, 8f, 8f, pFill);

            pFill.setColor(accentColor);
            canvas.drawRoundRect(new RectF(x, top + 6f, x + 3.5f, top + h - 6f), 2f, 2f, pFill);

            tpKpiNum.setColor(accentColor);
            canvas.drawText(valor, x + 12f, top + 24f, tpKpiNum);

            StaticLayout slLbl = crearLayout(label, tpKpiLabel, (int) (w - 18f));
            dibujarLayout(slLbl, x + 12f, top + 30f);
        }

        private void dibujarCalendarioYCronograma(
                Map<String, List<Tarea>> pendientesPorFecha,
                List<TareaConFecha> tareasPendientesConFecha
        ) {
            dibujarTituloSeccion("2. CALENDARIO MENSUAL DE TAREAS FALTANTES");

            Calendar mesCal = seleccionarMesCalendario(tareasPendientesConFecha);
            Calendar iter = (Calendar) mesCal.clone();
            iter.set(Calendar.DAY_OF_MONTH, 1);
            limpiarHoras(iter);

            int anio = iter.get(Calendar.YEAR);
            int mes = iter.get(Calendar.MONTH);
            int maxDias = iter.getActualMaximum(Calendar.DAY_OF_MONTH);
            int dow = iter.get(Calendar.DAY_OF_WEEK);
            int offset = (dow == Calendar.SUNDAY) ? 6 : (dow - Calendar.MONDAY);
            int numFilas = (int) Math.ceil((offset + maxDias) / 7.0);

            float calW = 284f;
            float cellH = 22f;
            float calHeaderH = 44f;
            float legendH = 22f;
            float calTotalH = calHeaderH + (numFilas * cellH) + legendH + 10f;

            asegurarEspacio(calTotalH + 20f, null);

            float startY = y;
            // Contenedor izquierdo: Calendario mensual
            RectF calCard = new RectF(MARGIN, startY, MARGIN + calW, startY + calTotalH);
            pFill.setColor(COLOR_WHITE);
            canvas.drawRoundRect(calCard, 8f, 8f, pFill);
            pStroke.setColor(COLOR_BORDER);
            canvas.drawRoundRect(calCard, 8f, 8f, pStroke);

            SimpleDateFormat sdfMes = new SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("es-PE"));
            String nombreMes = sdfMes.format(iter.getTime());
            if (!nombreMes.isEmpty()) {
                nombreMes = nombreMes.substring(0, 1).toUpperCase(Locale.ROOT) + nombreMes.substring(1);
            }

            // Banda título del mes
            pFill.setColor(COLOR_PRIMARY_SOFT);
            canvas.drawRoundRect(new RectF(MARGIN, startY, MARGIN + calW, startY + 24f), 8f, 8f, pFill);
            canvas.drawText(nombreMes, MARGIN + (calW / 2f) - (tpSubtitle.measureText(nombreMes) / 2f), startY + 16f, tpSubtitle);

            // Cabecera L M M J V S D
            String[] diasSem = {"L", "M", "M", "J", "V", "S", "D"};
            float innerX = MARGIN + 8f;
            float cellW = (calW - 16f) / 7f;
            float headerDaysY = startY + 37f;
            for (int i = 0; i < 7; i++) {
                float cx = innerX + (i * cellW) + (cellW / 2f);
                canvas.drawText(diasSem[i], cx - (tpSmallBold.measureText(diasSem[i]) / 2f), headerDaysY, tpSmallBold);
            }

            // Celdas de días
            int vencidasMes = 0;
            int proximasMes = 0;
            int futurasMes = 0;
            float gridTop = startY + calHeaderH;

            for (int d = 1; d <= maxDias; d++) {
                int pos = offset + (d - 1);
                int row = pos / 7;
                int col = pos % 7;

                float cellLeft = innerX + (col * cellW) + 2f;
                float cellTop = gridTop + (row * cellH) + 2f;
                float cellRight = innerX + ((col + 1) * cellW) - 2f;
                float cellBottom = gridTop + ((row + 1) * cellH) - 2f;

                String fIso = String.format(Locale.US, "%04d-%02d-%02d", anio, mes + 1, d);
                List<Tarea> delDia = pendientesPorFecha.get(fIso);

                int bgDia = Color.TRANSPARENT;
                int txtDia = COLOR_DARK_TEXT;
                boolean boldDia = false;

                if (delDia != null && !delDia.isEmpty()) {
                    boldDia = true;
                    Long diasObj = calcularDiasRestantes(fIso);
                    long dias = diasObj != null ? diasObj : 10;
                    if (dias <= 0) {
                        bgDia = COLOR_ERROR_BG;
                        txtDia = COLOR_ERROR;
                        vencidasMes += delDia.size();
                    } else if (dias <= 3) {
                        bgDia = COLOR_WARNING_BG;
                        txtDia = COLOR_WARNING;
                        proximasMes += delDia.size();
                    } else {
                        bgDia = COLOR_PRIMARY_SOFT;
                        txtDia = COLOR_PRIMARY;
                        futurasMes += delDia.size();
                    }
                }

                if (bgDia != Color.TRANSPARENT) {
                    pFill.setColor(bgDia);
                    canvas.drawRoundRect(new RectF(cellLeft, cellTop, cellRight, cellBottom), 4f, 4f, pFill);
                }

                TextPaint tpDia = new TextPaint(boldDia ? tpSmallBold : tpSmall);
                tpDia.setColor(txtDia);
                String numStr = String.valueOf(d);
                float tx = ((cellLeft + cellRight) / 2f) - (tpDia.measureText(numStr) / 2f);
                float ty = ((cellTop + cellBottom) / 2f) + 3f;
                canvas.drawText(numStr, tx, ty, tpDia);
            }

            // Leyenda compacta debajo de la cuadrícula
            float legY = startY + calTotalH - 14f;
            dibujarLeyendaItem(MARGIN + 10f, legY, COLOR_ERROR, "Vencida/Hoy");
            dibujarLeyendaItem(MARGIN + 98f, legY, COLOR_WARNING, "1-3 días");
            dibujarLeyendaItem(MARGIN + 178f, legY, COLOR_PRIMARY, "> 3 días");

            // Panel derecho junto al calendario: Resumen del mes mostrado
            float rightX = MARGIN + calW + 12f;
            float rightW = CONTENT_WIDTH - calW - 12f; // 227 pt
            RectF sideCard = new RectF(rightX, startY, rightX + rightW, startY + calTotalH);
            pFill.setColor(COLOR_ZEBRA);
            canvas.drawRoundRect(sideCard, 8f, 8f, pFill);
            pStroke.setColor(COLOR_BORDER);
            canvas.drawRoundRect(sideCard, 8f, 8f, pStroke);

            float sx = rightX + 12f;
            float sy = startY + 18f;
            canvas.drawText("PENDIENTES DEL MES", sx, sy, tpSmallBold);
            sy += 16f;
            dibujarFilaResumenCal(sx, sy, rightW - 24f, "Vencidas / Hoy:", String.valueOf(vencidasMes), COLOR_ERROR);
            sy += 18f;
            dibujarFilaResumenCal(sx, sy, rightW - 24f, "Próximas (1-3d):", String.valueOf(proximasMes), COLOR_WARNING);
            sy += 18f;
            dibujarFilaResumenCal(sx, sy, rightW - 24f, "Programadas (>3d):", String.valueOf(futurasMes), COLOR_PRIMARY);
            sy += 22f;

            String proximaStr = tareasPendientesConFecha.isEmpty()
                    ? "Sin entregas pendientes con fecha."
                    : "Próxima entrega: " + tareasPendientesConFecha.get(0).fechaIso + " (" +
                    (!TextUtils.isEmpty(tareasPendientesConFecha.get(0).tarea.getTitulo())
                            ? tareasPendientesConFecha.get(0).tarea.getTitulo() : "Tarea") + ")";
            StaticLayout slProx = crearLayout(proximaStr, tpSmall, (int) (rightW - 24f));
            dibujarLayout(slProx, sx, sy);

            y = startY + calTotalH + 14f;

            // Bloque Cronograma de tareas faltantes (ordenado por fecha_limite ASC)
            asegurarEspacio(36f, null);
            canvas.drawText("Cronograma de tareas faltantes (ordenado por fecha límite)", MARGIN, y + 10f, tpSubtitle);
            y += 18f;

            if (tareasPendientesConFecha.isEmpty()) {
                StaticLayout slVacio = crearLayout("No existen tareas pendientes con fecha límite asignada.", tpSmall, CONTENT_WIDTH);
                asegurarEspacio(slVacio.getHeight() + 10f, null);
                dibujarLayout(slVacio, MARGIN, y);
                y += slVacio.getHeight() + 14f;
                return;
            }

            final float[] colW = {72f, 225f, 126f, 100f}; // suma = 523
            final String[] colH = {"Fecha", "Tarea pendiente", "Responsable", "Plazo"};
            Runnable drawCronoHeader = () -> dibujarHeaderTabla(colW, colH);
            asegurarEspacio(24f, null);
            drawCronoHeader.run();

            for (int i = 0; i < tareasPendientesConFecha.size(); i++) {
                TareaConFecha item = tareasPendientesConFecha.get(i);
                String tTit = !TextUtils.isEmpty(item.tarea.getTitulo()) ? item.tarea.getTitulo().trim() : "Tarea";
                String tResp = !TextUtils.isEmpty(item.tarea.getResponsableNombre()) ? item.tarea.getResponsableNombre().trim() : "Sin asignar";
                String plazoTxt;
                int plazoColor;
                if (item.diasRestantes < 0) {
                    long atr = Math.abs(item.diasRestantes);
                    plazoTxt = "Atraso " + atr + (atr == 1 ? " día" : " días");
                    plazoColor = COLOR_ERROR;
                } else if (item.diasRestantes == 0) {
                    plazoTxt = "Vence HOY";
                    plazoColor = COLOR_ERROR;
                } else if (item.diasRestantes <= 3) {
                    plazoTxt = "Faltan " + item.diasRestantes + (item.diasRestantes == 1 ? " día" : " días");
                    plazoColor = COLOR_WARNING;
                } else {
                    plazoTxt = "Faltan " + item.diasRestantes + " días";
                    plazoColor = COLOR_PRIMARY;
                }

                StaticLayout slTit = crearLayout(tTit, tpSmall, (int) (colW[1] - 10f));
                StaticLayout slResp = crearLayout(tResp, tpSmall, (int) (colW[2] - 10f));
                float rowH = Math.max(20f, Math.max(slTit.getHeight(), slResp.getHeight()) + 8f);

                asegurarEspacio(rowH, drawCronoHeader);

                pFill.setColor(i % 2 == 0 ? COLOR_WHITE : COLOR_ZEBRA);
                canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + rowH, pFill);
                pStroke.setColor(COLOR_BORDER);
                canvas.drawLine(MARGIN, y + rowH, MARGIN + CONTENT_WIDTH, y + rowH, pStroke);

                float cx = MARGIN + 5f;
                canvas.drawText(item.fechaIso, cx, y + 13f, tpSmallBold);
                cx += colW[0];
                dibujarLayout(slTit, cx, y + 4f);
                cx += colW[1];
                dibujarLayout(slResp, cx, y + 4f);
                cx += colW[2];

                TextPaint tpPlazo = new TextPaint(tpSmallBold);
                tpPlazo.setColor(plazoColor);
                canvas.drawText(plazoTxt, cx, y + 13f, tpPlazo);

                y += rowH;
            }
            y += 16f;
        }

        private Calendar seleccionarMesCalendario(List<TareaConFecha> pendientes) {
            Calendar mesActual = (Calendar) hoyCal.clone();
            mesActual.set(Calendar.DAY_OF_MONTH, 1);

            int anioHoy = mesActual.get(Calendar.YEAR);
            int mesHoy = mesActual.get(Calendar.MONTH);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

            boolean hayEnMesActual = false;
            Date fechaProxima = null;
            Date fechaPrimera = null;

            for (TareaConFecha tcf : pendientes) {
                try {
                    Date d = sdf.parse(tcf.fechaIso);
                    if (d == null) continue;
                    if (fechaPrimera == null) fechaPrimera = d;

                    Calendar c = Calendar.getInstance();
                    c.setTime(d);
                    if (c.get(Calendar.YEAR) == anioHoy && c.get(Calendar.MONTH) == mesHoy) {
                        hayEnMesActual = true;
                        break;
                    }
                    if (!d.before(hoyCal.getTime()) && fechaProxima == null) {
                        fechaProxima = d;
                    }
                } catch (Exception ignored) {
                }
            }

            if (hayEnMesActual || (fechaProxima == null && fechaPrimera == null)) {
                return mesActual;
            }
            Calendar elegido = Calendar.getInstance();
            elegido.setTime(fechaProxima != null ? fechaProxima : fechaPrimera);
            elegido.set(Calendar.DAY_OF_MONTH, 1);
            limpiarHoras(elegido);
            return elegido;
        }

        private void dibujarLeyendaItem(float x, float baseY, int color, String texto) {
            pFill.setColor(color);
            canvas.drawCircle(x + 4f, baseY - 3f, 3.5f, pFill);
            canvas.drawText(texto, x + 11f, baseY, tpSmall);
        }

        private void dibujarFilaResumenCal(float x, float baseY, float w, String label, String val, int colorVal) {
            canvas.drawText(label, x, baseY, tpSmall);
            TextPaint tpVal = new TextPaint(tpSmallBold);
            tpVal.setColor(colorVal);
            canvas.drawText(val, x + w - tpVal.measureText(val), baseY, tpVal);
        }

        private void dibujarAvancePorFases(List<Fase> fases, List<Tarea> tareas) {
            dibujarTituloSeccion("3. AVANCE POR FASES (" + fases.size() + ")");

            if (fases.isEmpty()) {
                StaticLayout slVacio = crearLayout("No hay fases registradas en este proyecto.", tpSmall, CONTENT_WIDTH);
                asegurarEspacio(slVacio.getHeight() + 12f, null);
                dibujarLayout(slVacio, MARGIN, y);
                y += slVacio.getHeight() + 14f;
                return;
            }

            Map<String, int[]> statsFase = new HashMap<>();
            for (Tarea t : tareas) {
                if (t.getFaseId() == null) continue;
                int[] cnt = statsFase.get(t.getFaseId());
                if (cnt == null) {
                    cnt = new int[2]; // [0]=total, [1]=completadas
                    statsFase.put(t.getFaseId(), cnt);
                }
                cnt[0]++;
                if ("Finalizada".equalsIgnoreCase(t.getEstado())) {
                    cnt[1]++;
                }
            }

            for (Fase f : fases) {
                int[] cnt = statsFase.get(f.getId());
                int totalF = cnt != null ? cnt[0] : 0;
                int compF = cnt != null ? cnt[1] : 0;
                String estF = !TextUtils.isEmpty(f.getEstado()) ? f.getEstado().trim() : "Pendiente";
                int pctF = totalF > 0 ? (compF * 100) / totalF : ("Completada".equalsIgnoreCase(estF) ? 100 : 0);

                String nomFase = f.getOrden() + ". " + (!TextUtils.isEmpty(f.getNombre()) ? f.getNombre().trim() : "Fase");
                if (!TextUtils.isEmpty(f.getFechaEstimada())) {
                    nomFase += " (Est: " + f.getFechaEstimada().trim() + ")";
                }

                StaticLayout slNom = crearLayout(nomFase, tpBodyBold, CONTENT_WIDTH - 190);
                float cardH = Math.max(38f, slNom.getHeight() + 26f);
                asegurarEspacio(cardH + 6f, null);

                RectF card = new RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + cardH);
                pFill.setColor(COLOR_ZEBRA);
                canvas.drawRoundRect(card, 6f, 6f, pFill);
                pStroke.setColor(COLOR_BORDER);
                canvas.drawRoundRect(card, 6f, 6f, pStroke);

                dibujarLayout(slNom, MARGIN + 10f, y + 7f);

                // Badge estado + conteo a la derecha
                String infoDer = compF + "/" + totalF + " (" + pctF + "%)";
                float infoW = tpSmallBold.measureText(infoDer);
                canvas.drawText(infoDer, MARGIN + CONTENT_WIDTH - 10f - infoW, y + 17f, tpSmallBold);

                int estFg = "Completada".equalsIgnoreCase(estF) ? COLOR_SUCCESS
                        : ("EnProgreso".equalsIgnoreCase(estF) ? COLOR_PRIMARY : COLOR_MUTED_TEXT);
                int estBg = "Completada".equalsIgnoreCase(estF) ? COLOR_SUCCESS_BG
                        : ("EnProgreso".equalsIgnoreCase(estF) ? COLOR_PRIMARY_SOFT : COLOR_TRACK);
                dibujarBadge(MARGIN + CONTENT_WIDTH - 16f - infoW - 76f, y + 6f, 70f, 15f, estF, estBg, estFg);

                // Barra de progreso proporcional de la fase
                float barY = y + cardH - 13f;
                float barW = CONTENT_WIDTH - 20f;
                pFill.setColor(COLOR_TRACK);
                canvas.drawRoundRect(new RectF(MARGIN + 10f, barY, MARGIN + 10f + barW, barY + 6f), 3f, 3f, pFill);
                if (pctF > 0) {
                    float fW = Math.max(6f, (barW * pctF) / 100f);
                    pFill.setColor(pctF == 100 ? COLOR_SUCCESS : COLOR_ACCENT);
                    canvas.drawRoundRect(new RectF(MARGIN + 10f, barY, MARGIN + 10f + Math.min(barW, fW), barY + 6f), 3f, 3f, pFill);
                }

                y += cardH + 6f;
            }
            y += 10f;
        }

        private void dibujarTablaEquipo(List<MiembroEquipoItem> equipo, List<Tarea> tareas) {
            dibujarTituloSeccion("4. EQUIPO Y CARGA DE TRABAJO (" + equipo.size() + ")");

            if (equipo.isEmpty()) {
                StaticLayout slVacio = crearLayout("No hay miembros registrados en este proyecto.", tpSmall, CONTENT_WIDTH);
                asegurarEspacio(slVacio.getHeight() + 12f, null);
                dibujarLayout(slVacio, MARGIN, y);
                y += slVacio.getHeight() + 14f;
                return;
            }

            Map<String, CargaMiembro> cargaPorUsuario = new HashMap<>();
            for (Tarea t : tareas) {
                String respId = !TextUtils.isEmpty(t.getResponsableId()) ? t.getResponsableId() : t.getAsignadoA();
                if (TextUtils.isEmpty(respId)) continue;
                CargaMiembro cm = cargaPorUsuario.get(respId);
                if (cm == null) {
                    cm = new CargaMiembro();
                    cargaPorUsuario.put(respId, cm);
                }
                cm.asignadas++;
                if ("Finalizada".equalsIgnoreCase(t.getEstado())) {
                    cm.completadas++;
                } else {
                    cm.pendientes++;
                }
            }

            final float[] colW = {205f, 108f, 70f, 70f, 70f}; // suma = 523
            final String[] colH = {"Integrante", "Rol", "Asignadas", "Completadas", "Pendientes"};
            Runnable drawHeader = () -> dibujarHeaderTabla(colW, colH);
            asegurarEspacio(26f, null);
            drawHeader.run();

            for (int i = 0; i < equipo.size(); i++) {
                MiembroEquipoItem m = equipo.get(i);
                String uId = m.usuario != null ? m.usuario.getId() : null;
                String nom = (m.usuario != null && !TextUtils.isEmpty(m.usuario.getNombreCompleto()))
                        ? m.usuario.getNombreCompleto().trim() : "Usuario";
                String rolMostrar = cEquipo.esRolLider(m.rol) ? "Líder"
                        : (cEquipo.esRolObservador(m.rol) ? "Observador" : "Colaborador");

                CargaMiembro cm = uId != null ? cargaPorUsuario.get(uId) : null;
                int asig = cm != null ? cm.asignadas : 0;
                int comp = cm != null ? cm.completadas : 0;
                int pend = cm != null ? cm.pendientes : 0;

                StaticLayout slNom = crearLayout(nom, tpSmallBold, (int) (colW[0] - 10f));
                float rowH = Math.max(20f, slNom.getHeight() + 8f);
                asegurarEspacio(rowH, drawHeader);

                pFill.setColor(i % 2 == 0 ? COLOR_WHITE : COLOR_ZEBRA);
                canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + rowH, pFill);
                pStroke.setColor(COLOR_BORDER);
                canvas.drawLine(MARGIN, y + rowH, MARGIN + CONTENT_WIDTH, y + rowH, pStroke);

                float cx = MARGIN + 5f;
                dibujarLayout(slNom, cx, y + 4f);
                cx += colW[0];

                int rolBg = cEquipo.esRolLider(m.rol) ? COLOR_PRIMARY_SOFT
                        : (cEquipo.esRolObservador(m.rol) ? COLOR_TRACK : COLOR_SUCCESS_BG);
                int rolFg = cEquipo.esRolLider(m.rol) ? COLOR_PRIMARY
                        : (cEquipo.esRolObservador(m.rol) ? COLOR_MUTED_TEXT : COLOR_SUCCESS);
                dibujarBadge(cx, y + 3f, 82f, 14f, rolMostrar, rolBg, rolFg);
                cx += colW[1];

                canvas.drawText(String.valueOf(asig), cx + 18f, y + 13f, tpSmallBold);
                cx += colW[2];
                canvas.drawText(String.valueOf(comp), cx + 22f, y + 13f, tpSmall);
                cx += colW[3];
                canvas.drawText(String.valueOf(pend), cx + 20f, y + 13f, tpSmall);

                y += rowH;
            }
            y += 16f;
        }

        private void dibujarDetalleTareasPorFase(List<Fase> fases, List<Tarea> tareas) {
            dibujarTituloSeccion("5. DETALLE DE TAREAS POR FASE (" + tareas.size() + ")");

            if (tareas.isEmpty()) {
                StaticLayout slVacio = crearLayout("No hay tareas registradas en este proyecto.", tpSmall, CONTENT_WIDTH);
                asegurarEspacio(slVacio.getHeight() + 12f, null);
                dibujarLayout(slVacio, MARGIN, y);
                y += slVacio.getHeight() + 14f;
                return;
            }

            Map<String, List<Tarea>> tareasPorFase = new LinkedHashMap<>();
            Map<String, String> nombresFase = new LinkedHashMap<>();
            for (Fase f : fases) {
                tareasPorFase.put(f.getId(), new ArrayList<>());
                nombresFase.put(f.getId(), f.getOrden() + ". " + (!TextUtils.isEmpty(f.getNombre()) ? f.getNombre().trim() : "Fase"));
            }
            List<Tarea> sinFase = new ArrayList<>();
            for (Tarea t : tareas) {
                if (t.getFaseId() != null && tareasPorFase.containsKey(t.getFaseId())) {
                    tareasPorFase.get(t.getFaseId()).add(t);
                } else {
                    sinFase.add(t);
                }
            }
            if (!sinFase.isEmpty()) {
                tareasPorFase.put("__SIN_FASE__", sinFase);
                nombresFase.put("__SIN_FASE__", "Sin fase asignada");
            }

            final float[] colW = {163f, 100f, 68f, 74f, 68f, 50f}; // suma = 523
            final String[] colH = {"Tarea", "Responsable", "Prioridad", "Estado", "Límite", "Subt."};

            for (Map.Entry<String, List<Tarea>> entry : tareasPorFase.entrySet()) {
                List<Tarea> grupo = entry.getValue();
                if (grupo == null || grupo.isEmpty()) continue;

                String nombreGrupo = nombresFase.get(entry.getKey()) + " (" + grupo.size() + ")";
                Runnable drawTableHead = () -> dibujarHeaderTabla(colW, colH);

                // Asegurar espacio para sub-cabecera de fase + cabecera de tabla + 1 fila mínima
                asegurarEspacio(64f, null);
                pFill.setColor(COLOR_PRIMARY_SOFT);
                canvas.drawRoundRect(new RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + 18f), 4f, 4f, pFill);
                TextPaint tpGrp = new TextPaint(tpSmallBold);
                tpGrp.setColor(COLOR_PRIMARY);
                canvas.drawText(nombreGrupo, MARGIN + 8f, y + 12f, tpGrp);
                y += 20f;

                drawTableHead.run();

                for (int i = 0; i < grupo.size(); i++) {
                    Tarea t = grupo.get(i);
                    String titulo = !TextUtils.isEmpty(t.getTitulo()) ? t.getTitulo().trim() : "Tarea";
                    String resp = !TextUtils.isEmpty(t.getResponsableNombre()) ? t.getResponsableNombre().trim() : "Sin asignar";
                    String prio = !TextUtils.isEmpty(t.getPrioridad()) ? t.getPrioridad().trim() : "Media";
                    String est = !TextUtils.isEmpty(t.getEstado()) ? t.getEstado().trim() : "Pendiente";
                    String fLim = normalizarFechaIso(t.getFechaLimite());
                    String fShow = fLim != null ? fLim : "Sin fecha";
                    String subShow = t.getTotalSubtareas() > 0
                            ? t.getSubtareasCompletadas() + "/" + t.getTotalSubtareas()
                            : "—";

                    StaticLayout slTit = crearLayout(titulo, tpSmallBold, (int) (colW[0] - 8f));
                    StaticLayout slResp = crearLayout(resp, tpSmall, (int) (colW[1] - 8f));
                    float rowH = Math.max(22f, Math.max(slTit.getHeight(), slResp.getHeight()) + 8f);

                    asegurarEspacio(rowH, drawTableHead);

                    pFill.setColor(i % 2 == 0 ? COLOR_WHITE : COLOR_ZEBRA);
                    canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + rowH, pFill);
                    pStroke.setColor(COLOR_BORDER);
                    canvas.drawLine(MARGIN, y + rowH, MARGIN + CONTENT_WIDTH, y + rowH, pStroke);

                    float cx = MARGIN + 4f;
                    dibujarLayout(slTit, cx, y + 4f);
                    cx += colW[0];
                    dibujarLayout(slResp, cx, y + 4f);
                    cx += colW[1];

                    int pBg = ("Urgente".equalsIgnoreCase(prio) || "Alta".equalsIgnoreCase(prio)) ? COLOR_ERROR_BG
                            : ("Media".equalsIgnoreCase(prio) ? COLOR_WARNING_BG : COLOR_PRIMARY_SOFT);
                    int pFg = ("Urgente".equalsIgnoreCase(prio) || "Alta".equalsIgnoreCase(prio)) ? COLOR_ERROR
                            : ("Media".equalsIgnoreCase(prio) ? COLOR_WARNING : COLOR_PRIMARY);
                    dibujarBadge(cx, y + 4f, colW[2] - 8f, 14f, prio, pBg, pFg);
                    cx += colW[2];

                    int eBg = "Finalizada".equalsIgnoreCase(est) ? COLOR_SUCCESS_BG
                            : ("EnProgreso".equalsIgnoreCase(est) ? COLOR_PRIMARY_SOFT : COLOR_TRACK);
                    int eFg = "Finalizada".equalsIgnoreCase(est) ? COLOR_SUCCESS
                            : ("EnProgreso".equalsIgnoreCase(est) ? COLOR_PRIMARY : COLOR_MUTED_TEXT);
                    dibujarBadge(cx, y + 4f, colW[3] - 8f, 14f, est, eBg, eFg);
                    cx += colW[3];

                    canvas.drawText(fShow, cx, y + 14f, tpSmall);
                    cx += colW[4];
                    canvas.drawText(subShow, cx + 8f, y + 14f, tpSmallBold);

                    y += rowH;
                }
                y += 10f;
            }
            y += 6f;
        }

        private void dibujarAnexoEnlaces(List<EnlaceTarea> enlaces, List<Tarea> tareas) {
            if (enlaces == null || enlaces.isEmpty()) {
                dibujarTituloSeccion("6. ANEXO DE ENTREGABLES (ENLACES)");
                StaticLayout slVacio = crearLayout("No hay enlaces de entregables adjuntos en este proyecto.", tpSmall, CONTENT_WIDTH);
                asegurarEspacio(slVacio.getHeight() + 10f, null);
                dibujarLayout(slVacio, MARGIN, y);
                y += slVacio.getHeight() + 10f;
                return;
            }

            Map<String, String> tituloTareaPorId = new HashMap<>();
            for (Tarea t : tareas) {
                if (t.getId() != null) {
                    tituloTareaPorId.put(t.getId(), !TextUtils.isEmpty(t.getTitulo()) ? t.getTitulo().trim() : "Tarea");
                }
            }

            dibujarTituloSeccion("6. ANEXO DE ENTREGABLES (ENLACES) (" + enlaces.size() + ")");

            for (int i = 0; i < enlaces.size(); i++) {
                EnlaceTarea e = enlaces.get(i);
                String nomTarea = tituloTareaPorId.get(e.getTareaId());
                if (TextUtils.isEmpty(nomTarea)) nomTarea = "Tarea";
                String tituloEnl = !TextUtils.isEmpty(e.getTitulo()) ? e.getTitulo().trim() : "Entregable";
                String autor = !TextUtils.isEmpty(e.getAutorNombre()) ? e.getAutorNombre().trim() : "Usuario";
                String fecha = !TextUtils.isEmpty(e.getFechaCreacion()) ? " • " + e.getFechaCreacion().trim() : "";
                String url = !TextUtils.isEmpty(e.getUrl()) ? e.getUrl().trim() : "";

                StaticLayout slEnc = crearLayout("Tarea: " + nomTarea + "  —  " + tituloEnl + " (" + autor + fecha + ")",
                        tpSmallBold, CONTENT_WIDTH - 20);
                TextPaint tpUrl = new TextPaint(tpSmall);
                tpUrl.setColor(COLOR_ACCENT);
                StaticLayout slUrl = crearLayout(url, tpUrl, CONTENT_WIDTH - 20);

                float boxH = 8f + slEnc.getHeight() + 3f + slUrl.getHeight() + 8f;
                asegurarEspacio(boxH + 6f, null);

                RectF box = new RectF(MARGIN, y, MARGIN + CONTENT_WIDTH, y + boxH);
                pFill.setColor(i % 2 == 0 ? COLOR_ZEBRA : COLOR_WHITE);
                canvas.drawRoundRect(box, 6f, 6f, pFill);
                pStroke.setColor(COLOR_BORDER);
                canvas.drawRoundRect(box, 6f, 6f, pStroke);

                dibujarLayout(slEnc, MARGIN + 10f, y + 6f);
                dibujarLayout(slUrl, MARGIN + 10f, y + 6f + slEnc.getHeight() + 3f);

                y += boxH + 6f;
            }
        }

        private void dibujarTituloSeccion(String titulo) {
            asegurarEspacio(32f, null);
            canvas.drawText(titulo, MARGIN, y + 11f, tpSection);
            pStroke.setColor(COLOR_PRIMARY);
            pStroke.setStrokeWidth(1.2f);
            canvas.drawLine(MARGIN, y + 16f, MARGIN + CONTENT_WIDTH, y + 16f, pStroke);
            pStroke.setStrokeWidth(0.8f);
            pStroke.setColor(COLOR_BORDER);
            y += 24f;
        }

        private void dibujarHeaderTabla(float[] colW, String[] headers) {
            float headerH = 20f;
            pFill.setColor(COLOR_PRIMARY);
            canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + headerH, pFill);

            TextPaint tpHead = new TextPaint(tpSmallBold);
            tpHead.setColor(COLOR_WHITE);
            float cx = MARGIN + 5f;
            for (int i = 0; i < headers.length && i < colW.length; i++) {
                canvas.drawText(headers[i], cx, y + 13f, tpHead);
                cx += colW[i];
            }
            y += headerH;
        }

        private void dibujarBadge(float x, float top, float w, float h, String texto, int bgColor, int fgColor) {
            RectF rect = new RectF(x, top, x + w, top + h);
            pFill.setColor(bgColor);
            canvas.drawRoundRect(rect, 4f, 4f, pFill);

            TextPaint tpB = new TextPaint(tpSmallBold);
            tpB.setTextSize(7.5f);
            tpB.setColor(fgColor);
            String seguro = texto != null ? texto : "";
            while (seguro.length() > 3 && tpB.measureText(seguro) > w - 6f) {
                seguro = seguro.substring(0, seguro.length() - 2) + "…";
            }
            float tw = tpB.measureText(seguro);
            canvas.drawText(seguro, rect.centerX() - (tw / 2f), rect.top + 10f, tpB);
        }

        private StaticLayout crearLayout(String texto, TextPaint paint, int width) {
            String seguro = texto != null ? texto : "";
            return StaticLayout.Builder.obtain(seguro, 0, seguro.length(), paint, Math.max(10, width))
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
                    .setLineSpacing(1.5f, 1.0f)
                    .setIncludePad(false)
                    .build();
        }

        private void dibujarLayout(StaticLayout layout, float x, float top) {
            canvas.save();
            canvas.translate(x, top);
            layout.draw(canvas);
            canvas.restore();
        }

        private void asegurarEspacio(float altoRequerido, Runnable onNuevaPagina) {
            if (currentPage == null || y + altoRequerido > BOTTOM_LIMIT) {
                nuevaPagina();
                if (onNuevaPagina != null) {
                    onNuevaPagina.run();
                }
            }
        }

        private void nuevaPagina() {
            if (currentPage != null) {
                doc.finishPage(currentPage);
            }
            pageNum++;
            PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create();
            currentPage = doc.startPage(info);
            canvas = currentPage.getCanvas();

            // Pie de página discreto en todas las hojas
            pStroke.setColor(COLOR_BORDER);
            canvas.drawLine(MARGIN, 804f, MARGIN + CONTENT_WIDTH, 804f, pStroke);
            canvas.drawText("Emitido: " + fechaHoraEmision, MARGIN, 818f, tpFooter);
            String paginaTxt = "Gestia • Página " + pageNum;
            canvas.drawText(paginaTxt, MARGIN + CONTENT_WIDTH - tpFooter.measureText(paginaTxt), 818f, tpFooter);

            y = TOP_Y;
        }

        void finalizar() {
            if (currentPage != null) {
                doc.finishPage(currentPage);
                currentPage = null;
            }
        }

        private String normalizarFechaIso(String fechaStr) {
            if (fechaStr == null) return null;
            String trim = fechaStr.trim();
            if (trim.length() < 10) return null;
            String iso = trim.substring(0, 10);
            if (iso.charAt(4) != '-' || iso.charAt(7) != '-') return null;
            return iso;
        }

        private Long calcularDiasRestantes(String fechaIso) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                sdf.setLenient(false);
                Date d = sdf.parse(fechaIso);
                if (d == null) return null;
                Calendar cal = Calendar.getInstance();
                cal.setTime(d);
                limpiarHoras(cal);
                long diff = cal.getTimeInMillis() - hoyCal.getTimeInMillis();
                return Math.round((double) diff / 86400000.0);
            } catch (Exception e) {
                return null;
            }
        }

        private void limpiarHoras(Calendar cal) {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
        }
    }
}
