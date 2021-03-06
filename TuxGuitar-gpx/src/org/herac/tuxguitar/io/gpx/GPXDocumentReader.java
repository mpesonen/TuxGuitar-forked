package org.herac.tuxguitar.io.gpx;

import java.io.InputStream;
import java.math.BigDecimal;

import javax.xml.parsers.DocumentBuilderFactory;

import org.herac.tuxguitar.graphics.control.TGLyricImpl;
import org.herac.tuxguitar.io.gpx.score.*;
import org.herac.tuxguitar.song.factory.TGFactory;
import org.herac.tuxguitar.song.models.TGLyric;
import org.herac.tuxguitar.song.models.TGStroke;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;

public class GPXDocumentReader {
	
	private Document xmlDocument;
	private GPXDocument gpxDocument;

	public GPXDocumentReader(InputStream stream){
		this.xmlDocument = getDocument(stream);
		this.gpxDocument = new GPXDocument();

		// DEBUG: printing the XML might be informative
		//System.out.println(getStringFromDoc(this.xmlDocument));
	}
	
	private Document getDocument(InputStream stream) {
		try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
		} catch (Throwable throwable) {
			throwable.printStackTrace();
		}
		return null;
	}
	
	public GPXDocument read(){
		if( this.xmlDocument != null ){
			this.readScore();
			this.readAutomations();
			this.readTracks();
			this.readMasterBars();
			this.readBars();
			this.readVoices();
			this.readBeats();
			this.readNotes();
			this.readRhythms();

			postProcessChordDiagrams();
			postProcessLyrics();
		}
		return this.gpxDocument;
	}

	private void postProcessChordDiagrams() {
		// Basically the trackId has to be decoded in order to know which diagram relates to which GPXBeat
		for (GPXBeat beat : this.gpxDocument.getBeats()) {
            if (beat.getHasChordDiagram()) {
                int beatId = beat.getId();

                int chordId = beat.getChordDiagramId();

                for (GPXVoice voice : this.gpxDocument.getVoices()) {
                    for (int voiceBeatId : voice.getBeatIds()){
                        if (voiceBeatId == beatId) {
                            int voiceId = voice.getId();

                            for (GPXBar bar : this.gpxDocument.getBars())
                            {
                                for (int barVoiceId : bar.getVoiceIds())
                                {
                                    if (barVoiceId == voiceId) {
                                        int barId = bar.getId();

                                        for (GPXMasterBar masterBar : this.gpxDocument.getMasterBars()) {
											int[] barIds = masterBar.getBarIds();
                                            for (int i=0; i < barIds.length; i++) {
                                                if (barIds[i] == barId) {
                                                    int trackId = i;

                                                    for (GPXChordDiagram possibleChordDiagram : this.gpxDocument.getTracks().get(trackId).getChordDiagrams()) {
                                                    	if (possibleChordDiagram.getId() == chordId)
														{
															beat.setChordName(possibleChordDiagram.getName());
															beat.setChordDiagram(possibleChordDiagram);
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

                }
            }
        }
	}

	private void postProcessLyrics() {
		int lyricTrackIndex = -1;

		// Basically the trackId has to be decoded in order to know which lyric line relates to which GPXBeat
		findTrackIndexLoop:
		for (GPXBeat beat : this.gpxDocument.getBeats()) {
			if (beat.getLyricsLines().size() > 0) {
				int beatId = beat.getId();

				for (GPXVoice voice : this.gpxDocument.getVoices()) {
					for (int voiceBeatId : voice.getBeatIds()){
						if (voiceBeatId == beatId) {
							int voiceId = voice.getId();

							for (GPXBar bar : this.gpxDocument.getBars())
							{
								for (int barVoiceId : bar.getVoiceIds())
								{
									if (barVoiceId == voiceId) {
										int barId = bar.getId();

										for (GPXMasterBar masterBar : this.gpxDocument.getMasterBars()) {
											int[] barIds = masterBar.getBarIds();
											for (int i=0; i < barIds.length; i++) {
												if (barIds[i] == barId) {
													lyricTrackIndex = i;
													break findTrackIndexLoop;
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
		}

		if (lyricTrackIndex > -1) {
			TGLyric lyrics = new TGLyricImpl();
			lyrics.setFrom(lyricTrackIndex);

			// Find all ids of beats with lyrics
			List<Integer> beatsWithLyricsIds = new ArrayList<Integer>();
			for (GPXBeat beat : this.gpxDocument.getBeats()) {
				if (beat.getLyricsLines().size() > 0) {
					beatsWithLyricsIds.add(new Integer(beat.getId()));
				}
			}

			List<Integer> decodedBeatsWithLyricsIds = new ArrayList<Integer>();
			// Find all voices with beats with lyrics
			for(GPXVoice voice : this.gpxDocument.getVoices())
			{
				for (int voiceBeatId : voice.getBeatIds()) {
					for (Integer beatWithLyric : beatsWithLyricsIds) {
						if (beatWithLyric.intValue() == voiceBeatId) {
							decodedBeatsWithLyricsIds.add(beatWithLyric);
						}
					}
				}
			}

			String[] lyricLines = new String[] {"", "", "", "", ""};

			for (Integer decodedBeatWithLyric : decodedBeatsWithLyricsIds) {
				for (GPXBeat beat : this.gpxDocument.getBeats()) {
					int lyricsLinesCount = beat.getLyricsLines().size();
					if (decodedBeatWithLyric.intValue() == beat.getId()) {
						for (int i = 0; i < lyricsLinesCount; i++) {
							lyricLines[i] += beat.getLyricsLines().get(i) + "\n";
						}
					}
				}
			}

			// TGLyrics only handles one line of lyrics as of now
			lyrics.setLyrics(lyricLines[0]);

			gpxDocument.getTracks().get(lyricTrackIndex).setLyrics(lyrics);
		}
	}

	public void readScore(){
		if( this.xmlDocument != null ){
			Node scoreNode = getChildNode(this.xmlDocument.getFirstChild(), "Score");
			if( scoreNode != null ){
				this.gpxDocument.getScore().setTitle( getChildNodeContent(scoreNode, "Title"));
				this.gpxDocument.getScore().setSubTitle( getChildNodeContent(scoreNode, "SubTitle"));
				this.gpxDocument.getScore().setArtist( getChildNodeContent(scoreNode, "Artist"));
				this.gpxDocument.getScore().setAlbum( getChildNodeContent(scoreNode, "Album"));
				this.gpxDocument.getScore().setWords( getChildNodeContent(scoreNode, "Words"));
				this.gpxDocument.getScore().setMusic( getChildNodeContent(scoreNode, "Music"));
				this.gpxDocument.getScore().setWordsAndMusic( getChildNodeContent(scoreNode, "WordsAndMusic"));
				this.gpxDocument.getScore().setCopyright( getChildNodeContent(scoreNode, "Copyright"));
				this.gpxDocument.getScore().setTabber( getChildNodeContent(scoreNode, "Tabber"));
				this.gpxDocument.getScore().setInstructions( getChildNodeContent(scoreNode, "Instructions"));
				this.gpxDocument.getScore().setNotices( getChildNodeContent(scoreNode, "Notices"));
			}
		}
	}
	
	public void readAutomations(){
		if( this.xmlDocument != null ){
			Node masterTrackNode = getChildNode(this.xmlDocument.getFirstChild(), "MasterTrack");
			if( masterTrackNode != null ){
				NodeList automationNodes = getChildNodeList(masterTrackNode, "Automations");
				for( int i = 0 ; i < automationNodes.getLength() ; i ++ ){
					Node automationNode = automationNodes.item( i );
					if( automationNode.getNodeName().equals("Automation") ){
						GPXAutomation automation = new GPXAutomation();
						automation.setType( getChildNodeContent(automationNode, "Type"));
						automation.setBarId( getChildNodeIntegerContent(automationNode, "Bar"));
						automation.setValue( getChildNodeFloatContentArray(automationNode, "Value"));
						automation.setLinear( getChildNodeBooleanContent(automationNode, "Linear"));
						automation.setPosition( getChildNodeFloatContent(automationNode, "Position"));
						automation.setVisible( getChildNodeBooleanContent(automationNode, "Visible"));
						
						this.gpxDocument.getAutomations().add( automation );
					}
				}
			}
		}
	}
	
	public void readTracks(){
		if( this.xmlDocument != null ){
			NodeList trackNodes = getChildNodeList(this.xmlDocument.getFirstChild(), "Tracks");
			for( int i = 0 ; i < trackNodes.getLength() ; i ++ ){
				Node trackNode = trackNodes.item( i );
				if( trackNode.getNodeName().equals("Track") ){
					GPXTrack track = new GPXTrack();
					track.setId( getAttributeIntegerValue(trackNode, "id") );
					track.setName(getChildNodeContent(trackNode, "Name" ));
					track.setColor(getChildNodeIntegerContentArray(trackNode, "Color"));
					Node gmNode = getChildNode(trackNode, "GeneralMidi");
					if( gmNode != null ){
						track.setGmProgram(getChildNodeIntegerContent(gmNode, "Program"));
						track.setGmChannel1(getChildNodeIntegerContent(gmNode, "PrimaryChannel"));
						track.setGmChannel2(getChildNodeIntegerContent(gmNode, "SecondaryChannel"));
					}
					
					NodeList propertyNodes = getChildNodeList(trackNode, "Properties");
					if( propertyNodes != null ){
						for( int p = 0 ; p < propertyNodes.getLength() ; p ++ ){
							Node propertyNode = propertyNodes.item( p );
							if (propertyNode.getNodeName().equals("Property") ){ 
								if( getAttributeValue(propertyNode, "name").equals("Tuning") ){
									track.setTunningPitches( getChildNodeIntegerContentArray(propertyNode, "Pitches") );
								}

								if( getAttributeValue(propertyNode, "name").equals("CapoFret") ){
									track.setCapoOffset(getChildNodeIntegerContent(propertyNode, "Fret"));
								}

								// Read chord diagram collection
								if ( getAttributeValue(propertyNode, "name").equals("DiagramCollection")) {
									NodeList diagramCollectionItems = getChildNodeList(propertyNode, "Items");
									if (diagramCollectionItems != null) {
										for (int diagId=0; diagId < diagramCollectionItems.getLength(); diagId++) {
											Node chordItemNode = diagramCollectionItems.item(diagId);

											if (chordItemNode.getNodeName() != null && chordItemNode.getNodeName().equals("Item"))
											{
												int chordItemId = getAttributeIntegerValue(chordItemNode, "id");
												String chordItemName = getAttributeValue(chordItemNode, "name");

												GPXChordDiagram chordDiagram = new GPXChordDiagram(chordItemId, chordItemName);

												Node chordItemDiagram = getChildNode(chordItemNode, "Diagram");
												NodeList chordDiagramChildren = chordItemDiagram.getChildNodes();

												chordDiagram.setFirstFret(getAttributeIntegerValue(chordItemDiagram, "baseFret"));
												int stringCount = getAttributeIntegerValue(chordItemDiagram, "stringCount");
												for (int chordDiagramChildIndex = 0; chordDiagramChildIndex < chordDiagramChildren.getLength(); chordDiagramChildIndex++) {
													Node chordDiagramChildNode = chordDiagramChildren.item(chordDiagramChildIndex);

													if (chordDiagramChildNode.getNodeName().equals("Fret")) {
														chordDiagram.addFret(new Integer(getAttributeIntegerValue(chordDiagramChildNode, "fret")));
													}
												}
												Collections.reverse(chordDiagram.getFrets());

												track.addChordDiagram(chordDiagram);
											}
										}
									}
								}
							}
						}
					}
					this.gpxDocument.getTracks().add( track );
				}
			}
		}
	}
	
	public void readMasterBars(){
		if( this.xmlDocument != null ){
			NodeList masterBarNodes = getChildNodeList(this.xmlDocument.getFirstChild(), "MasterBars");
			for( int i = 0 ; i < masterBarNodes.getLength() ; i ++ ){
				Node masterBarNode = masterBarNodes.item( i );
				if( masterBarNode.getNodeName().equals("MasterBar") ){
					GPXMasterBar masterBar = new GPXMasterBar();
					masterBar.setBarIds( getChildNodeIntegerContentArray(masterBarNode, "Bars"));
					masterBar.setTime( getChildNodeIntegerContentArray(masterBarNode, "Time", "/"));
					masterBar.setTripletFeel(getChildNodeContent(masterBarNode, "TripletFeel"));
					
					Node repeatNode = getChildNode(masterBarNode, "Repeat");
					if( repeatNode != null ){
						masterBar.setRepeatStart(getAttributeBooleanValue(repeatNode, "start"));
						if( getAttributeBooleanValue(repeatNode, "end") ){
							masterBar.setRepeatCount( getAttributeIntegerValue(repeatNode, "count"));
						}
					}
					
					Node keyNode = getChildNode(masterBarNode, "Key");
					if (keyNode != null) {
						masterBar.setAccidentalCount(this.getChildNodeIntegerContent(keyNode, "AccidentalCount") ); 
						masterBar.setMode(this.getChildNodeContent(keyNode, "Mode") ); 
					}

					Node sectionNode = getChildNode(masterBarNode, "Section");
					if (sectionNode != null) {
						masterBar.setMarkerLetter(this.getChildNodeContent(sectionNode, "Letter").trim() );
						masterBar.setMarkerText(this.getChildNodeContent(sectionNode, "Text").trim() );
					}
					
					this.gpxDocument.getMasterBars().add( masterBar );
				}
			}
		}
	}
	
	public void readBars(){
		if( this.xmlDocument != null ){
			NodeList barNodes = getChildNodeList(this.xmlDocument.getFirstChild(), "Bars");
			for( int i = 0 ; i < barNodes.getLength() ; i ++ ){
				Node barNode = barNodes.item( i );
				if( barNode.getNodeName().equals("Bar") ){
					GPXBar bar = new GPXBar();
					bar.setId(getAttributeIntegerValue(barNode, "id"));
					bar.setVoiceIds( getChildNodeIntegerContentArray(barNode, "Voices"));
					bar.setClef(getChildNodeContent(barNode, "Clef"));
					bar.setSimileMark(getChildNodeContent(barNode,"SimileMark"));
					
					this.gpxDocument.getBars().add( bar );
				}
			}
		}
	}
	
	public void readVoices(){
		if( this.xmlDocument != null ){
			NodeList voiceNodes = getChildNodeList(this.xmlDocument.getFirstChild(), "Voices");
			for( int i = 0 ; i < voiceNodes.getLength() ; i ++ ){
				Node voiceNode = voiceNodes.item( i );
				if( voiceNode.getNodeName().equals("Voice") ){
					GPXVoice voice = new GPXVoice();
					voice.setId(getAttributeIntegerValue(voiceNode, "id"));
					voice.setBeatIds( getChildNodeIntegerContentArray(voiceNode, "Beats"));
					
					this.gpxDocument.getVoices().add( voice );
				}
			}
		}
	}

	public String getStringFromDoc(org.w3c.dom.Document doc) {
		try
		{
			DOMSource domSource = new DOMSource(doc);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.transform(domSource, result);
			writer.flush();
			return writer.toString();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
			return null;
		}
	}
	
	public void readBeats(){
		if( this.xmlDocument != null ){
			NodeList beatNodes = getChildNodeList(this.xmlDocument.getFirstChild(), "Beats");
			for( int i = 0 ; i < beatNodes.getLength() ; i ++ ){
				Node beatNode = beatNodes.item( i );
				if( beatNode.getNodeName().equals("Beat") ) {
					GPXBeat beat = new GPXBeat();
					beat.setId(getAttributeIntegerValue(beatNode, "id"));
					beat.setDynamic(getChildNodeContent(beatNode, "Dynamic"));
					beat.setRhythmId(getAttributeIntegerValue(getChildNode(beatNode, "Rhythm"), "ref"));
					beat.setTremolo( getChildNodeIntegerContentArray(beatNode, "Tremolo", "/"));
					// Reading "arpeggio region"
					beat.setText ( getChildNodeContent(beatNode, "FreeText") );
					beat.setNoteIds( getChildNodeIntegerContentArray(beatNode, "Notes"));
					String chordNode = getChildNodeContent(beatNode, "Chord" );

					// Reading chord
					if (chordNode != null && !chordNode.isEmpty()) {
						beat.setHasChordDiagram ( true );
						beat.setChordDiagramId( Integer.parseInt(chordNode) );
					}

					// Reading lyrics
					NodeList lyricsLineNodes = getChildNodeList(beatNode, "Lyrics");
					if (lyricsLineNodes != null) {
						for (int lyricIndex = 0; lyricIndex < lyricsLineNodes.getLength(); lyricIndex++) {
							Node lyricNode = lyricsLineNodes.item(lyricIndex);
							if (lyricNode.getNodeName() != null && lyricNode.getNodeName().equals("Line")) {
								if (lyricNode.getTextContent() != null)
									beat.addLyricsLine( lyricNode.getTextContent() );
							}
						}
					}

					NodeList propertyNodes = getChildNodeList(beatNode, "Properties");
					if( propertyNodes != null ){
						for( int p = 0 ; p < propertyNodes.getLength() ; p ++ ){
							Node propertyNode = propertyNodes.item( p );
							if (propertyNode.getNodeName().equals("Property") ){ 
								String propertyName = getAttributeValue(propertyNode, "name");

								if (propertyName.equals("PickStroke")) {
									String propertyValue = new String();
									propertyValue = getChildNodeContent(propertyNode, "Direction");

									if (propertyValue.toUpperCase().equals("UP"))
										beat.setPickStrokeType(TGStroke.STROKE_UP);
									else if (propertyValue.toUpperCase().equals("DOWN"))
										beat.setPickStrokeType(TGStroke.STROKE_DOWN);
									else
										beat.setPickStrokeType(TGStroke.STROKE_NONE);
								}
								if( propertyName.equals("WhammyBar") ){
									beat.setWhammyBarEnabled( getChildNode(propertyNode, "Enable") != null );
								}
								if( propertyName.equals("WhammyBarOriginValue") ){
									beat.setWhammyBarOriginValue( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("WhammyBarMiddleValue") ){
									beat.setWhammyBarMiddleValue( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("WhammyBarDestinationValue") ){
									beat.setWhammyBarDestinationValue( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("WhammyBarOriginOffset") ){
									beat.setWhammyBarOriginOffset( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("WhammyBarMiddleOffset1") ){
									beat.setWhammyBarMiddleOffset1( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("WhammyBarMiddleOffset2") ){
									beat.setWhammyBarMiddleOffset2( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("WhammyBarDestinationOffset") ){
									beat.setWhammyBarDestinationOffset( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
							}
						}
					}
					
					this.gpxDocument.getBeats().add( beat );
				}
			}
		}
	}
	
	public void readNotes(){
		if( this.xmlDocument != null ){
			NodeList noteNodes = getChildNodeList(this.xmlDocument.getFirstChild(), "Notes");
			for( int i = 0 ; i < noteNodes.getLength() ; i ++ ){
				Node noteNode = noteNodes.item( i );
				if( noteNode.getNodeName().equals("Note") ){
					GPXNote note = new GPXNote();
					note.setId( getAttributeIntegerValue(noteNode, "id") );
					
					Node tieNode = getChildNode(noteNode, "Tie");
					note.setTieDestination( tieNode != null ? getAttributeValue(tieNode, "destination").equals("true") : false);
					
					String ghostNodeContent = getChildNodeContent(noteNode, "AntiAccent");
					if( ghostNodeContent != null ){
						note.setGhost(ghostNodeContent.equals("Normal"));
					}
					
					note.setAccent(getChildNodeIntegerContent(noteNode, "Accent"));
					note.setTrill(getChildNodeIntegerContent(noteNode, "Trill"));

					// Reading fingerings
					String leftFingering = getChildNodeContent(noteNode, "LeftFingering");
					if (leftFingering != null && !leftFingering.isEmpty()) {
						note.setLeftFingering( leftFingering );
					}
					String rightFingering = getChildNodeContent(noteNode, "RightFingering");
					if (rightFingering != null && !rightFingering.isEmpty()) {
						note.setRightFingering( rightFingering );
					}

					// Reading accidentals
					String accidentalString = getChildNodeContent(noteNode, "Accidental");
					if (accidentalString != null && !accidentalString.isEmpty()) {
						try
						{
							GPXNote.Accidental accidental = GPXNote.Accidental.valueOf(accidentalString);
							note.setAccidental(accidental);
						}
						catch (Exception e)
						{
							System.out.println("Unexpected accidental value for " + accidentalString);
						}
					}

					note.setVibrato( getChildNode(noteNode, "Vibrato") != null );
					
					NodeList propertyNodes = getChildNodeList(noteNode, "Properties");
					if( propertyNodes != null ){
						for( int p = 0 ; p < propertyNodes.getLength() ; p ++ ){
							Node propertyNode = propertyNodes.item( p );
							if (propertyNode.getNodeName().equals("Property") ){ 
								String propertyName = getAttributeValue(propertyNode, "name");
								if( propertyName.equals("String") ){
									note.setString( getChildNodeIntegerContent(propertyNode, "String") );
								}
								if( propertyName.equals("Fret") ){
									note.setFret( getChildNodeIntegerContent(propertyNode, "Fret") );
								}
								if( propertyName.equals("Midi") ){
									note.setMidiNumber( getChildNodeIntegerContent(propertyNode, "Number") );
								}
								if( propertyName.equals("Tone") ){
									note.setTone( getChildNodeIntegerContent(propertyNode, "Step") );
								}
								if( propertyName.equals("Octave") ){
									note.setOctave( getChildNodeIntegerContent(propertyNode, "Number") );
								}
								if( propertyName.equals("Element") ){
									note.setElement( getChildNodeIntegerContent(propertyNode, "Element") );
								}
								if( propertyName.equals("Variation") ){
									note.setVariation( getChildNodeIntegerContent(propertyNode, "Variation") );
								}
								if( propertyName.equals("Muted") ){
									note.setMutedEnabled( getChildNode(propertyNode, "Enable") != null );
								}
								if( propertyName.equals("PalmMuted") ){
									note.setPalmMutedEnabled( getChildNode(propertyNode, "Enable") != null );
								}
								if( propertyName.equals("Slide") ){
									note.setSlide( true );
									note.setSlideFlags( getChildNodeIntegerContent(propertyNode, "Flags") );
								}
								if( propertyName.equals("Tapped") ){
									note.setTapped( getChildNode(propertyNode, "Enable") != null );
								}
								if( propertyName.equals("Bended") ){
									note.setBendEnabled( getChildNode(propertyNode, "Enable") != null );
								}
								if( propertyName.equals("BendOriginValue") ){
									note.setBendOriginValue( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("BendMiddleValue") ){
									note.setBendMiddleValue( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("BendDestinationValue") ){
									note.setBendDestinationValue( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("BendOriginOffset") ){
									note.setBendOriginOffset( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("BendMiddleOffset1") ){
									note.setBendMiddleOffset1( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("BendMiddleOffset2") ){
									note.setBendMiddleOffset2( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("BendDestinationOffset") ){
									note.setBendDestinationOffset( new Integer(getChildNodeIntegerContent(propertyNode, "Float")) );
								}
								if( propertyName.equals("HopoOrigin") ){
									note.setHammer(true);
								}
								if( propertyName.equals("HopoDestination") ){
	//								this is a hammer-on or pull-off
								}
								if( propertyName.equals("HarmonicFret") ){
									note.setHarmonicFret( ( getChildNodeIntegerContent(propertyNode, "HFret") ) );
								}
								if( propertyName.equals("HarmonicType") ){
									note.setHarmonicType( getChildNodeContent (propertyNode, "HType"));
								}
							}
						}
					}
					
					this.gpxDocument.getNotes().add( note );
				}
			}
		}
	}
	
	public void readRhythms(){
		if( this.xmlDocument != null ){
			NodeList rhythmNodes = getChildNodeList(this.xmlDocument.getFirstChild(), "Rhythms");
			for( int i = 0 ; i < rhythmNodes.getLength() ; i ++ ){
				Node rhythmNode = rhythmNodes.item( i );
				if( rhythmNode.getNodeName().equals("Rhythm") ){
					Node primaryTupletNode = getChildNode(rhythmNode, "PrimaryTuplet");
					Node augmentationDotNode = getChildNode(rhythmNode, "AugmentationDot");
					
					GPXRhythm rhythm = new GPXRhythm();
					rhythm.setId( getAttributeIntegerValue(rhythmNode, "id") );
					rhythm.setNoteValue(getChildNodeContent(rhythmNode, "NoteValue") );
					rhythm.setPrimaryTupletDen(primaryTupletNode != null ? getAttributeIntegerValue(primaryTupletNode, "den") : 1);
					rhythm.setPrimaryTupletNum(primaryTupletNode != null ? getAttributeIntegerValue(primaryTupletNode, "num") : 1);
					rhythm.setAugmentationDotCount(augmentationDotNode != null ? getAttributeIntegerValue(augmentationDotNode, "count") : 0);
					
					this.gpxDocument.getRhythms().add( rhythm );
				}
			}
		}
	}
	
	private String getAttributeValue(Node node, String attribute ){
		if( node != null ){
			return node.getAttributes().getNamedItem( attribute ).getNodeValue();
		}
		return null;
	}
	
	private int getAttributeIntegerValue(Node node, String attribute ){
		try {
			return new BigDecimal(this.getAttributeValue(node, attribute)).intValue();
		} catch( Throwable throwable ){ 
			return 0;
		}
	}
	
	private boolean getAttributeBooleanValue(Node node, String attribute ){
		String value = this.getAttributeValue(node, attribute);
		if( value != null ){
			return value.equals("true");
		}
		return false;
	}
	
	private Node getChildNode(Node node, String name ){
		NodeList childNodes = node.getChildNodes();
		for( int i = 0 ; i < childNodes.getLength() ; i ++ ){
			Node childNode = childNodes.item( i );
			if( childNode.getNodeName().equals( name ) ){
				return childNode;
			}
		}
		return null;
	}
	
	private NodeList getChildNodeList(Node node, String name ){
		Node childNode = getChildNode(node, name);
		if( childNode != null ){
			return childNode.getChildNodes();
		}
		return null;
	}
	
	private String getChildNodeContent(Node node, String name ){
		Node childNode = getChildNode(node, name);
		if( childNode != null ){
			return childNode.getTextContent();
		}
		return null;
	}
	
	private boolean getChildNodeBooleanContent(Node node, String name ){
		String value = this.getChildNodeContent(node, name);
		if( value != null ){
			return value.equals("true");
		}
		return false;
	}
	
	private int getChildNodeIntegerContent(Node node, String name){
		try {
			return new BigDecimal(this.getChildNodeContent(node, name)).intValue();
		} catch( Throwable throwable ){
			return 0;
		}
	}

	private float getChildNodeFloatContent(Node node, String name){
		try {
			return new BigDecimal(this.getChildNodeContent(node, name)).floatValue();
		} catch( Throwable throwable ){
			return 0;
		}
	}
	
	private int[] getChildNodeIntegerContentArray(Node node, String name , String regex){
		String rawContents = this.getChildNodeContent(node, name);
		if( rawContents != null ){
			String[] contents = rawContents.trim().split(regex);
			int[] intContents = new int[contents.length];
			for( int i = 0 ; i < intContents.length; i ++ ){
				try {
					intContents[i] = new BigDecimal( contents[i].trim() ).intValue();
				} catch( Throwable throwable ){
					intContents[i] = 0;
				}
			}
			return intContents;
		}
		return null;
	}
	
	private int[] getChildNodeIntegerContentArray(Node node, String name ){
		return getChildNodeIntegerContentArray(node, name, (" ") );
	}

	private float[] getChildNodeFloatContentArray(Node node, String name , String regex){
		String rawContents = this.getChildNodeContent(node, name);
		if( rawContents != null ){
			String[] contents = rawContents.trim().split(regex);
			float[] floatContents = new float[contents.length];
			for( int i = 0 ; i < floatContents.length; i ++ ){
				try {
					floatContents[i] = new BigDecimal( contents[i].trim() ).floatValue();
				} catch( Throwable throwable ){
					floatContents[i] = 0;
				}
			}
			return floatContents;
		}
		return null;
	}

	private float[] getChildNodeFloatContentArray(Node node, String name ){
		return getChildNodeFloatContentArray(node, name, (" ") );
	}
}
