#!/usr/bin/env php
<?php

function main()
{
    // define a dictionary
    $words = array();
    $words_h = fopen("words", 'r');
    while ($line = fgets($words_h)) {
        array_push($words, trim($line));
    }

    // connect to MySQL
    echo "MySQL username: ";
    $user = trim(fgets(STDIN));
    echo "MySQL password: ";
    $pass = trim(fgets(STDIN));
    $mysqli = mysqli_init();
    //$mysqli->options();
    $mysqli->real_connect('myleott.com', $user, $pass, 'restaurant_reviews', NULL, NULL, MYSQLI_CLIENT_COMPRESS);
    //$mysqli = new mysqli('myleott.com', $user, $pass, 'restaurant_reviews');

    // generate random reviews
    $nreviews = 10000;
    $reviews = array();
    for ($i = 0; $i < $nreviews; $i++) {
        $review = array(
            'id' => $i,
            'author_id' => "---1lKK3aKOuomHnwAkAow",
            'restaurant_id' => "101 Deli-1982288894",
            'rating' => rand(1, 5),
            'usefulCounter' => rand(1, 100),
            'funnyCounter' => rand(1, 100),
            'coolCounter' => rand(1, 100),
            'text' => gen_random_review($words, rand(20, 200))
        );
        array_push($reviews, $review);
    }

    // insert compressed
    $start_time = microtime(true);
    insert_random_reviews($mysqli, "YelpReview", $reviews);
    $mysqli->commit();
    $end_time = microtime(true);
    $time = $end_time - $start_time;
    echo "Compressed: inserted $nreviews reviews in $time seconds\n";

    // insert uncompressed
    $start_time = microtime(true);
    insert_random_reviews($mysqli, "YelpReview_uncompressed", $reviews);
    $mysqli->commit();
    $end_time = microtime(true);
    $time = $end_time - $start_time;
    echo "Uncompressed: inserted $nreviews reviews in $time seconds\n";

    // close MySQL connection
    $mysqli->close();
}

function gen_random_review($words, $n)
{
    // generate list of random indices in $words
    $word_indices = array_rand($words, $n);

    // map indices to words and save them in $review
    $review = array();
    foreach ($word_indices as $word_index) {
        array_push($review, $words[$word_index]);
    }

    // combine review with the space character
    return implode(" ", $review);
}

// first try inserting compressed reviews
function insert_random_reviews($mysqli, $table, $reviews)
{
    // prepare statement
    $stmt = $mysqli->prepare("INSERT INTO $table (id, author_id, restaurant_id, rating, usefulCounter, funnyCounter, coolCounter, text) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");

    // bind variables to parameters
    $id            = "";
    $author_id     = "";
    $restaurant_id = "";
    $rating        = -1;
    $usefulCounter = -1;
    $funnyCounter  = -1;
    $coolCounter   = -1;
    $text          = "";
    $stmt->bind_param('sssdiiis', $id, $author_id, $restaurant_id, $rating, $usefulCounter, $funnyCounter, $coolCounter, $text);

    foreach ($reviews as $review) {
        // assign $review values to bound variables
        $id            = $review['id'];
        $author_id     = $review['author_id'];
        $restaurant_id = $review['restaurant_id'];
        $rating        = $review['rating'];
        $usefulCounter = $review['usefulCounter'];
        $funnyCounter  = $review['funnyCounter'];
        $coolCounter   = $review['coolCounter'];
        $text          = $review['text'];

        // insert into DB
        $stmt->execute();
    } 

    $stmt->close();
}

main();

?>
