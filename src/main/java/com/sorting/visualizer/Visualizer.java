package com.sorting.visualizer;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Arrays;
import java.util.Random;

public class Visualizer {

    private final Canvas canvas;
    private final GraphicsContext gc;

    private int[] barLengths;
    private boolean[] isSorted;
    private int totalBars;

    private int highlight1 = -1, highlight2 = -1;
    private boolean horizontalMode = false;
    private Runnable onDraw = null;

    private volatile boolean paused = false;
    private volatile double speed = 18;

    public Visualizer(int numBars) {
        this.totalBars = numBars;
        this.barLengths = new int[numBars];
        this.isSorted = new boolean[numBars];

        this.canvas = new Canvas(800, 600);
        this.gc = canvas.getGraphicsContext2D();
    }

    public Canvas getCanvas() { return canvas; }
    public void setHorizontal(boolean horizontal) { this.horizontalMode = horizontal; }
    public void setOnDraw(Runnable r) { this.onDraw = r; }
    public void setSpeed(double s) { speed = s; }

    public synchronized void setBarCount(int count) {
        this.totalBars = count;
        this.barLengths = new int[count];
        this.isSorted = new boolean[count];
    }

    public synchronized int[] getBarsCopy() {
        return Arrays.copyOf(barLengths, barLengths.length);
    }

    public synchronized void shuffle() {
        Random rand = new Random();
        int max = horizontalMode ? (int) canvas.getWidth() - 20 : (int) canvas.getHeight() - 20;
        int min = Math.max(3, max / 15);

        for (int i = 0; i < totalBars; i++) {
            barLengths[i] = rand.nextInt(max - min + 1) + min;
            isSorted[i] = false;
        }

        highlight1 = highlight2 = -1;
        draw();
    }

    public void draw() {
        Platform.runLater(() -> {
            gc.setFill(Color.web("#f8f8f8"));
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

            if (horizontalMode) drawHorizontal();
            else drawVertical();

            if (onDraw != null) onDraw.run();
        });
    }

    private synchronized void drawVertical() {
        double barWidth = canvas.getWidth() / totalBars;
        double canvasHeight = canvas.getHeight();

        for (int i = 0; i < totalBars; i++) {
            double h = barLengths[i];
            gc.setFill(isSorted[i] ? Color.LIMEGREEN : (i == highlight1 || i == highlight2 ? Color.ORANGE : Color.CORNFLOWERBLUE));
            gc.fillRect(i * barWidth + 1, canvasHeight - h, barWidth - 2, h);
            gc.setStroke(Color.web("#2b2b2b", 0.08));
            gc.strokeRect(i * barWidth + 1, canvasHeight - h, barWidth - 2, h);
        }
    }

    private synchronized void drawHorizontal() {
        double barHeight = canvas.getHeight() / totalBars;

        for (int i = 0; i < totalBars; i++) {
            double w = barLengths[i];
            gc.setFill(isSorted[i] ? Color.LIMEGREEN : (i == highlight1 || i == highlight2 ? Color.ORANGE : Color.CORNFLOWERBLUE));
            gc.fillRect(0, i * barHeight + 1, w, barHeight - 2);
            gc.setStroke(Color.web("#2b2b2b", 0.08));
            gc.strokeRect(0, i * barHeight + 1, w, barHeight - 2);
        }
    }

    private void checkPause() throws InterruptedException {
        while(paused) Thread.sleep(50);
    }

    private void sleep() throws InterruptedException {
        checkPause();
        Thread.sleep((long) speed);
    }

    public void setPaused(boolean p) { paused = p; }

    private synchronized void swap(int i,int j){ int t=barLengths[i]; barLengths[i]=barLengths[j]; barLengths[j]=t; }
    private synchronized void markAllSorted(){ Arrays.fill(isSorted,true); }
    private synchronized void clearHighlights(){ highlight1=highlight2=-1; }

    // --- SORTING METHODS ---
    public void bubbleSort() {
        try {
            for(int i=0;i<totalBars-1;i++){
                boolean swapped=false;
                for(int j=0;j<totalBars-i-1;j++){
                    synchronized(this){ highlight1=j; highlight2=j+1; if(barLengths[j]>barLengths[j+1]) { swap(j,j+1); swapped=true; } }
                    draw(); sleep();
                }
                synchronized(this){ isSorted[totalBars-1-i]=true; }
                if(!swapped) break;
            }
            markAllSorted();
        } catch(InterruptedException ignored) {}
        finally { clearHighlights(); draw(); }
    }

    public void insertionSort() {
        try {
            for(int i=1;i<totalBars;i++){
                int key; synchronized(this){ key=barLengths[i]; }
                int j=i-1;
                while(true){
                    int left; synchronized(this){ left=(j>=0?barLengths[j]:Integer.MIN_VALUE); highlight1=j; highlight2=j+1; }
                    if(j>=0 && left>key){ synchronized(this){ barLengths[j+1]=left; } j--; }
                    else { synchronized(this){ barLengths[j+1]=key; } break; }
                    draw(); sleep();
                }
                synchronized(this){ for(int k=0;k<=i;k++) isSorted[k]=true; } draw();
            }
            markAllSorted();
        } catch(InterruptedException ignored) {}
        finally { clearHighlights(); draw(); }
    }

    public void selectionSort() {
        try {
            for(int i=0;i<totalBars-1;i++){
                int min=i;
                for(int j=i+1;j<totalBars;j++){
                    synchronized(this){ highlight1=min; highlight2=j; if(barLengths[j]<barLengths[min]) min=j; }
                    draw(); sleep();
                }
                synchronized(this){ swap(i,min); isSorted[i]=true; } draw();
            }
            markAllSorted();
        } catch(InterruptedException ignored) {}
        finally { clearHighlights(); draw(); }
    }

    public void mergeSort() {
        try { mergeSortHelper(0,totalBars-1); markAllSorted(); } catch(InterruptedException ignored) {}
        finally { clearHighlights(); draw(); }
    }

    private void mergeSortHelper(int l,int r) throws InterruptedException{
        if(l>=r) return;
        int m=(l+r)/2;
        mergeSortHelper(l,m); mergeSortHelper(m+1,r); merge(l,m,r);
    }

    private void merge(int l,int m,int r) throws InterruptedException{
        int n1=m-l+1, n2=r-m;
        int[] left=new int[n1], right=new int[n2];
        synchronized(this){ System.arraycopy(barLengths,l,left,0,n1); System.arraycopy(barLengths,m+1,right,0,n2); }
        int i=0,j=0,k=l;
        while(i<n1 && j<n2){
            synchronized(this){ highlight1=k; highlight2=l+i; }
            if(left[i]<=right[j]){ synchronized(this){ barLengths[k]=left[i]; } i++; } else { synchronized(this){ barLengths[k]=right[j]; } j++; }
            k++; draw(); sleep();
        }
        while(i<n1){ synchronized(this){ highlight1=k; barLengths[k]=left[i]; } i++; k++; draw(); sleep(); }
        while(j<n2){ synchronized(this){ highlight1=k; barLengths[k]=right[j]; } j++; k++; draw(); sleep(); }
    }

    public void quickSort() {
        try{ quickSortHelper(0,totalBars-1); markAllSorted(); } catch(InterruptedException ignored) {}
        finally{ clearHighlights(); draw(); }
    }

    private void quickSortHelper(int l,int h) throws InterruptedException{
        if(l<h){
            int pi=partition(l,h);
            quickSortHelper(l,pi-1);
            quickSortHelper(pi+1,h);
        }
    }

    private int partition(int l,int h) throws InterruptedException{
        int pivot; synchronized(this){ pivot=barLengths[h]; }
        int i=l-1;
        for(int j=l;j<h;j++){
            synchronized(this){ highlight1=j; highlight2=h; }
            if(barLengths[j]<pivot){ i++; synchronized(this){ swap(i,j); } }
            draw(); sleep();
        }
        synchronized(this){ swap(i+1,h); } draw(); sleep();
        return i+1;
    }
}
