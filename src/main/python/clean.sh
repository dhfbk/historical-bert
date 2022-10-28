#!/bin/bash

for folder in */*/*; do
    if [ -d "$folder/odt" ]; then
        for format in $folder/*; do
            format_only=${format: -3}
            if [ "$format_only" != "odt" ]; then
                rm -r $format
                # echo "odt $format";
            fi;
        done;
    elif [ -d "$folder/txt" ]; then
        for format in $folder/*; do
            format_only=${format: -3}
            if [ "$format_only" != "txt" ]; then
                rm -r $format
                # echo "txt $format";
            fi;
        done;
    elif [ -d "$folder/rtf" ]; then
        for format in $folder/*; do
            format_only=${format: -3}
            if [ "$format_only" != "rtf" ]; then
                rm -r $format
                # echo "rtf $format";
            fi;
        done;
    fi;
done;
