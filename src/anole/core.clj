(ns anole.core
  (:require [clojure.java.io :as io]
            [clojure.math.numeric-tower :as math]
            [clojure.set :as set])
  (:import [java.io File FileNotFoundException]
  		     [java.awt Color]
           [java.awt.image BufferedImage]
           [javax.imageio ImageIO]))

(defn read-image
  "Takes a fully-qualified file name and reads it,
  returning a java.awt.image.BufferedImage."
  [file-name]
  (ImageIO/read (new File file-name)))

(defn write-image
  "Takes a java.awt.image.BufferedImage and
  a file name and writes it out as a PNG."
  [buffered-image file-name]
  (ImageIO/write buffered-image "png" (new File file-name)))

(defn copy-image
  "Takes a java.awt.image.BufferedImage and returns
  a full copy of it (not just a new reference)."
  [buffered-image]
  (let [color-model (.getColorModel buffered-image)]
    (new BufferedImage
         color-model
         (.copyData buffered-image nil)
         (.isAlphaPremultiplied color-model)
         nil)))

(defn- get-rgb-array
  [buffered-image]
  (let [height (.getHeight buffered-image)
        width (.getWidth buffered-image)]
    (.getRGB buffered-image 0 0 width height nil 0 width)))

(defn- test-color-range
  [number]
  (if (< number 0)
    0
    (if (> number 255)
      255
      (math/round number))))

(defn- get-aa-set [buffered-image main-colors-ints]
  (let [rgb-set (set (get-rgb-array buffered-image))
        main-colors-set (set main-colors-ints)]
    (set/difference rgb-set main-colors-set)))

(defn- find-closest-color
  [pixel-int main-colors-ints]
  (let [pixel-color (new Color pixel-int true)
        pr (.getRed pixel-color)
        pg (.getGreen pixel-color)
        pb (.getBlue pixel-color)
        main-colors (map #(new Color % true) main-colors-ints)
        diffs-seq (map #(hash-map
                         :color %
                         :diff (+ (math/abs (- pr (.getRed %)))
                                  (math/abs (- pg (.getGreen %)))
                                  (math/abs (- pb (.getBlue %)))))
                       main-colors)]
    (->
     (sort-by :diff diffs-seq)
     (first)
     (:color)
     (.getRGB))))

(defn- find-swap-color
  [pixel-int main-colors-swap-map]
  (if (= 0 pixel-int)
    0
    (let [main-colors-originals (keys main-colors-swap-map)
          closest-original-color (find-closest-color pixel-int main-colors-originals)
          main-swap-color (main-colors-swap-map closest-original-color)
          original-as-color (new Color closest-original-color true)
          main-swap-as-color (new Color main-swap-color true)
          pixel-as-color (new Color pixel-int true)
          red-diff (- (.getRed original-as-color) (.getRed pixel-as-color))
          green-diff (- (.getGreen original-as-color) (.getGreen pixel-as-color))
          blue-diff (- (.getBlue original-as-color) (.getBlue pixel-as-color))]
      (->
       (new Color
          (test-color-range (- (.getRed main-swap-as-color) red-diff))
          (test-color-range (- (.getGreen main-swap-as-color) green-diff))
          (test-color-range (- (.getBlue main-swap-as-color) blue-diff))
          (.getAlpha pixel-as-color))
       (.getRGB)))))

(defn- construct-swap-map
  [aa-set main-colors-swap-map]
  (let [aa-swap-map (->> (map (fn [i] (vector i (find-swap-color i main-colors-swap-map))) aa-set)
                        (flatten)
                        (apply hash-map))]
    (merge main-colors-swap-map aa-swap-map)))

(defn alter-image
  "Takes a java.awt.image.BufferedImage and a map of original/new
  color pairs (as integers) and returns a new java.awt.image.BufferedImage
  with the original colors specified in the map swapped with their
  new replacements. Anti-aliased pixels from the original image are
  also altered to correspond to their new colors. Note that any original
  colors NOT specified in the map are treated as anti-aliased pixels and
  modified accordingly. If there is an original color that you wish to retain,
  include it in the map with its paired new color being the same color."
  [buffered-image main-colors-swap-map]
  (let [rgb-array (get-rgb-array buffered-image)
        new-image (copy-image buffered-image)
        swap-map (construct-swap-map
                  (get-aa-set buffered-image (keys main-colors-swap-map))
                  main-colors-swap-map)]
    (do
      (.setRGB new-image 0 0 (.getWidth new-image) (.getHeight new-image)
               (int-array (replace swap-map rgb-array)) 0 (.getWidth new-image))
      new-image)))



;now it is as simple as (replace swap-map rgb-array)
